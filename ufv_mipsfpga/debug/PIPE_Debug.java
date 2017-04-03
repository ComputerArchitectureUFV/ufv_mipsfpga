/* 
 * 24/05/2016
 * String Debug Component. - UFV MG Brasil
 * Jeronimo Costa Penha - jeronimopenha@gmail.com
 */
package ufv_mipsfpga.debug;

import hades.models.PortStdLogic1164;
import hades.models.PortStdLogicVector;
import hades.models.StdLogic1164;
import hades.models.StdLogicVector;
import hades.signals.Signal;
import hades.signals.SignalStdLogic1164;
import hades.simulator.Port;
import hades.simulator.SimEvent;
import hades.simulator.SimEvent1164;
import hades.symbols.BboxRectangle;
import hades.symbols.BusPortSymbol;
import hades.symbols.InstanceLabel;
import hades.symbols.Label;
import hades.symbols.Polyline;
import hades.symbols.PortLabel;
import hades.symbols.PortSymbol;
import hades.symbols.Rectangle;
import hades.symbols.Symbol;
import jfig.objects.FigAttribs;
import ufv_mipsfpga.ColorLabel;

public class PIPE_Debug extends hades.models.rtlib.GenericRtlibObject {

    protected String string;
    protected Label stringLabel;
    protected Rectangle background;
    protected double t_delay;

    protected PortStdLogicVector port_A, port_OUT;
    protected PortStdLogic1164 port_RST, port_CLK;

    public PIPE_Debug() {
        super();
        string = "NOP";
        t_delay = 10.0E-8;
    }

    @Override
    protected void constructPorts() {
        port_A = new PortStdLogicVector(this, "A", Port.IN, null, 32);
        port_RST = new PortStdLogic1164(this, "RES", Port.IN, null);
        port_CLK = new PortStdLogic1164(this, "CLK", Port.IN, null);
        port_OUT = new PortStdLogicVector(this, "O", Port.OUT, null, 32);

        ports = new Port[4];
        ports[0] = port_A;
        ports[1] = port_RST;
        ports[2] = port_CLK;
        ports[3] = port_OUT;
    }

    public void setString(String s) {
        this.string = s;
        stringLabel.setText(s);
        getSymbol().painter.paint(getSymbol(), 100);
    }

    public String getString() {
        return this.string;
    }

    private int ret_imediate(String im) {
        String temp = "";
        int imediato;

        if (im.substring(0, 1).equals("1")) {
            for (int i = 0; i < 16; i++) {
                if (im.substring(i, i + 1).equals("1")) {
                    temp = temp + "0";
                } else {
                    temp = temp + "1";
                }
            }
            imediato = bin_to_int(temp, 16) + 1;
            imediato *= -1;
        } else {
            imediato = bin_to_int(im, 16);
        }
        return imediato;
    }

    private int bin_to_int(String im, int l) {//converte binario para decimal
        int soma = 0, cont = 0;
        for (int i = l; i > 0; i--) {
            if (im.substring(i - 1, i).equals("1")) {
                soma += (int) Math.pow(2, cont);
            }
            cont++;
        }
        return soma;
    }

    @Override
    public void evaluate(Object arg) {
        double time;

        int i, tamanho;
        String temp, str = "";

        Signal signal_A, signal_O;

        boolean isX = false;

        if (port_CLK.getSignal() == null) {
            isX = true;
        } else if (port_RST.getSignal() == null) {
            isX = true;
        } else if (port_A.getSignal() == null) {
            isX = true;
        } else if (port_OUT.getSignal() == null) {
            isX = true;
        }

        StdLogic1164 value_RST = port_RST.getValueOrU();

        if (isX || value_RST.is_1()) {
            str = "NOP";
            setString(str);

            //para port_DATA_OUT
            if ((signal_O = port_OUT.getSignal()) != null) { // get output
                vector.setValue(0);
                time = simulator.getSimTime() + delay;
                simulator.scheduleEvent(new SimEvent(signal_O, time, vector, port_OUT));
            }
        } else {

            SignalStdLogic1164 clk = (SignalStdLogic1164) port_CLK.getSignal();

            if (clk.hasRisingEdge()) {

                StdLogicVector value_A;

                signal_A = port_A.getSignal();

                value_A = (StdLogicVector) signal_A.getValue();
                temp = value_A.toString();
                tamanho = temp.length();

                for (i = 0; i < tamanho; i++) {  //busca o índice do primeiro bit na string
                    if (temp.substring(i, i + 1).equals(":")) {
                        break;
                    }
                }

                int opcode = bin_to_int(temp.substring(i + 1, i + 7), 6);//retorna o valor de opcode inteiro

                switch (opcode) {
                    case 35: // LW
                        str = "LW " + "R" + bin_to_int(temp.substring(i + 12, i + 17), 5) + ", " + ret_imediate(temp.substring(i + 17, i + 33)) + "(R" + bin_to_int(temp.substring(i + 7, i + 12), 5) + ")";
                        break;
                    case 43: // SW
                        str = "SW " + "R" + bin_to_int(temp.substring(i + 12, i + 17), 5) + ", " + ret_imediate(temp.substring(i + 17, i + 33)) + "(R" + bin_to_int(temp.substring(i + 7, i + 12), 5) + ")";
                        break;
                    case 4: //BEQ
                        str = "BEQ " + "R" + bin_to_int(temp.substring(i + 12, i + 17), 5) + ", " + ret_imediate(temp.substring(i + 17, i + 33)) + "(R" + bin_to_int(temp.substring(i + 7, i + 12), 5) + ")";
                        break;
                    case 0: // R Type

                        int func = bin_to_int(temp.substring(i + 27, i + 33), 6);

                        switch (func) {
                            case 32://add
                                str = "ADD " + "R" + bin_to_int(temp.substring(i + 17, i + 22), 5) + ", R" + bin_to_int(temp.substring(i + 7, i + 12), 5) + ", R" + bin_to_int(temp.substring(i + 12, i + 17), 5);
                                break;
                            case 34://sub
                                str = "SUB " + "R" + bin_to_int(temp.substring(i + 17, i + 22), 5) + ", R" + bin_to_int(temp.substring(i + 7, i + 12), 5) + ", R" + bin_to_int(temp.substring(i + 12, i + 17), 5);
                                break;
                            case 36://and
                                str = "AND " + "R" + bin_to_int(temp.substring(i + 17, i + 22), 5) + ", R" + bin_to_int(temp.substring(i + 7, i + 12), 5) + ", R" + bin_to_int(temp.substring(i + 12, i + 17), 5);
                                break;
                            case 37://or
                                str = "OR " + "R" + bin_to_int(temp.substring(i + 17, i + 22), 5) + ", R" + bin_to_int(temp.substring(i + 7, i + 12), 5) + ", R" + bin_to_int(temp.substring(i + 12, i + 17), 5);
                                break;
                            case 42://slt
                                str = "SLT " + "R" + bin_to_int(temp.substring(i + 17, i + 22), 5) + ", R" + bin_to_int(temp.substring(i + 7, i + 12), 5) + ", R" + bin_to_int(temp.substring(i + 12, i + 17), 5);
                                break;
                            default:
                                str = "NOP";
                        }
                        break;
                    default:
                        str = "NOP";
                }

                setString(str);

                signal_O = port_OUT.getSignal();
                StdLogicVector saida = new StdLogicVector(32);
                saida.setValue(value_A.getValue());
                time = simulator.getSimTime() + t_delay;
                simulator.scheduleEvent(SimEvent1164.createNewSimEvent(signal_O, time, saida, port_OUT));

            }
        }
    }

    @Override
    public boolean needsDynamicSymbol() {
        return true;
    }

    @Override
    public void constructDynamicSymbol() {
        symbol = new Symbol();
        symbol.setParent(this);

        BboxRectangle bbr = new BboxRectangle();
        bbr.initialize("0 -600 4000 600");

        InstanceLabel ilabel = new InstanceLabel();
        ilabel.initialize("4100 200 " + getName());

        BusPortSymbol busportsymbol0 = new BusPortSymbol();
        busportsymbol0.initialize("-600 0 A");

        BusPortSymbol busportsymbol1 = new BusPortSymbol();
        busportsymbol1.initialize("5400 0 O");

        PortSymbol portsymbolclk = new PortSymbol();
        portsymbolclk.initialize("0 600 CLK");

        PortSymbol portsymbolres = new PortSymbol();
        portsymbolres.initialize("600 600 RES");

        PortLabel portlabel = new PortLabel();
        portlabel.initialize("-400 0 A");

        PortLabel portlabelO = new PortLabel();
        portlabelO.initialize("5000 0 O");

        PortLabel portlabelclk = new PortLabel();
        portlabelclk.initialize("0 400 CLK");

        PortLabel portlabelres = new PortLabel();
        portlabelres.initialize("600 400 RES");

        stringLabel = new ColorLabel();
        stringLabel.initialize("0 200 " + getString());

        background = new Rectangle();
        background.initialize("0 -600 4000 600");
        jfig.objects.FigAttribs attr = background.getAttributes();
        attr.currentLayer = 50;
        attr.lineColor = null;
        attr.fillColor = java.awt.Color.white;
        attr.fillStyle = FigAttribs.SOLID_FILL;
        background.setAttributes(attr);

        Polyline border = new Polyline();
        border.initialize("7 0 -600 -600 -600 -600 600 4800 600 5400 0 4800 -600 0 -600");

        symbol.addMember(ilabel);
        symbol.addMember(busportsymbol0);
        symbol.addMember(busportsymbol1);
        symbol.addMember(portsymbolclk);
        symbol.addMember(portsymbolres);
        symbol.addMember(portlabel);
        symbol.addMember(portlabelO);
        symbol.addMember(portlabelclk);
        symbol.addMember(portlabelres);
        symbol.addMember(stringLabel);
        symbol.addMember(background);
        symbol.addMember(border);
        symbol.addMember(bbr);
    }
}
