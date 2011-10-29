package pez.nn.nrlibj;
import java.io.*;
import java.lang.*;
//import java.lang.Exception;
import java.util.*;
import java.util.zip.*;

import robocode.*;

/************************************************************************/
/*                                                                      */
/*                                                                      */
/*                 CLASS  NNet                                          */
/*                                                                      */
/*                                                                      */
/************************************************************************/

/**
* This Class (the most important) defines a Neural Network object.
* A NNet object contains a lot of computation nodes linked toghether in various
* ways but organized in layers.<BR>
* So, a NNet is an array of layers.<p>
* The computing phase proceeds from the layer 0 to last layer.<BR>
* For each layer:<BR>
* each node is computed appling its "trf" function.<BR>
* After that, before pass to next layer, again for each node is applied the
* "out" node function. This function moves the output value from the out-buffer
* to out variable and sincronize layer computation.
* (for the EBP phase, if used, the process is reversed using err
* variable).<p>
* A Node can be defined by user or can be one of predefined node type (NodeLin
* or NodeSigm). But a layer have same type nodes.<p>
* A layer can have a buffer. This buffer is another layer. A layer can have just
* one buffer but this buffer can have a buffer... in a chain way.
* When a layer have a buffer, its output values are copied into the input
* variable of destination layer nodes.<BR>
* These values will be computed when this buffer will be computed. So, if this
* buffer have a number &lt than the buffered layer these values will be considered
* in next forward cycle as a memory, otherwise in the same computational cycle.
* <PRE>
*         layer 0              layer 2       layer 3
*          |--|                 |--|          |--|
*          |  |---------------->|  |--------->|  |
*          |  |             |-->|  |          |  |
*          |  |        |--| |   |--|          |--|
*          |--|        |  |-|     ¦buffered
*                      |  |       ¦layer
*               layer 1|--|       ¦
*      buffer of layer2  ^........¦
*
* </PRE>
*
* @author D. Denaro
* @version 5.0 , 2/2001
*/
public class NNet
{
 Layer lyr[];

 NNet(){}
 /**
 * This constructor return a NNet with <TT>"nlayer"</TT> layers empty
 */
 NNet(int nlayer){this.lyr=new Layer[nlayer];}

 /**
 * This constructor creates a NNet with tree layers
 * and a buffer memory (0,1,2,3) or without memory (0,1,2).
 * <BR>When <TT>"mem"</TT> = true, a buffer of hiden layer is created and this
 * memory layer is linked with the hiden layer (Elman context memory)
 * <BR>The weights of the net are randomicaly choosen with range <TT>"ra"</TT> - <TT>"rb"</TT>
 * <BR><TT>"nodeinp"</TT> is the number of first layer nodes,
 * <BR><TT>"nodehid"</TT> of hiden layer and
 * <BR><TT>"nodeout"</TT> of last layer.
 * <BR>The first layer nodes (layer 0) are liner node (NodeLin); hidden layer
 * and last layer nodes have sigmoid transfer function of activation (NodeSigm)
 */
 public NNet(int nnodeinp,int nnodehid,int nnodeout,boolean fmem,float ra,float rb)
 {int i; String CnodeLin, CnodeSigm ;int nlyr;
  {CnodeLin="NodeLin";CnodeSigm="NodeSigm";}
  if (fmem) nlyr=4; else nlyr=3;
  lyr= new Layer[nlyr];
  lyr[0]=new Layer(0,nnodeinp,CnodeLin);
  if (fmem)
  {
   lyr[1]=new Layer(1,nnodehid,CnodeSigm);
   lyr[2]=new Layer(2,nnodehid,CnodeSigm);
   lyr[2].mbuff=lyr[1];
   lyr[3]=new Layer(3,nnodeout,CnodeSigm);
  }
  else
  {
   lyr[1]=new Layer(1,nnodehid,CnodeSigm);
   lyr[2]=new Layer(2,nnodeout,CnodeSigm);
  }
  if (fmem)
  {lyr[2].linkFromAll(lyr[0],ra,rb);lyr[2].linkFromAll(lyr[1],ra,rb);lyr[3].linkFromAll(lyr[2],ra,rb);}
  else
  {lyr[1].linkFromAll(lyr[0],ra,rb);lyr[2].linkFromAll(lyr[1],ra,rb);}
 }

 /**
 * This constructor creates a NNet with tree layers (0,1,3)
 * and a buffer memory or not. But the
 * the weights of the net are randomicaly choosen with default range
 */
 public NNet(int nnodeinp,int nnodehid,int nnodeout,boolean fmem)
 {this(nnodeinp,nnodehid,nnodeout,fmem,NrPop.ra,NrPop.rb);}

 /**
 * This constructor creates a simple NNet with tree layers (0,1,3) without context
 * memory. Default range for random choosen weights
 */
 public NNet(int nnodeinp,int nnodehid,int nnodeout)
 {this(nnodeinp,nnodehid,nnodeout,false,NrPop.ra,NrPop.rb);}

 /**
 * This constructor creates a network in a most flexible way.
 * The string array <TT>"descrrec"</TT> contains the records that describes the network
 * population in a simple predefined laguage:
 * <PRE>
 * 3 records types
 *
 * <B>
 *  layer=n [tnode=n,[m]  nname=xxxx... copytoml=n] </B>
 *   meens: definition of layer number "n" with "n" total-node (or n x m nodes
 *          if is a bidimesional layer) and with a buffer realised by layer "n"
 *          The node type is defined by the name (Ex. NodeLin, NodeSigm)
 * <B>
 *  linktype=[xxx...] fromlayer=n[(h[,k])]  tolayer=n[(h[,k])] [ value=na,[nb]] </B>
 *   meens: link "all" to all (default) or "one" to one from layer number "n" to
 *          layer number "n". As default link is realized between all layers
 *          node, but it is possible to describe a link for a sub-set of nodes
 *          from node "h" to node "k". As default weights are randomically chosen
 *          in default range, but it is possible define a different range between
 *          "na" and "nb". If only "na" is present, that meens all weights = "na"
 * <B>
 *  biasval=na[,nb]  oflayer=n[(h[,k])] </B>
 *   meens: as previous kind of record but for bias values
 *
 * example of descrrec[] contents (each record = one array element)
 *
 * layer=0 tnode=2  nname=NodeLin
 * layer=1 tnode=2  nname=NodeSigm
 * layer=2 tnode=1  nname=NodeSigm
 * linktype=all fromlayer=0  tolayer=1
 * linktype=all fromlayer=1  tolayer=2
 * </PRE>
 * it defines a network with 3 layers and 2,2,1 nodes (for XOR problem for instance)
 * NodeLin is a kind of node that reproduce the input value into the output variable
 * NodeSigm is a sigmoid node.
 */
 public NNet(String descrrec[])
 { this(descrrec,0);}

 /**
 * This constructor is similar to NNet(String descrrec[]) but read net
 * description from a file. (for an external NN definition)
 */
 public NNet(String namefile)
 {this(NrPop.fileToStrArray(namefile),0);}

 NNet(String descrrec[],int s)
 {
  int i,k,ct;
  StringTokenizer tok;
  String t,val,ThisC,Pack;
  String a,b,c;
  ThisC=this.getClass().getName();
  Pack=ThisC.substring(0,ThisC.lastIndexOf('.')+1);
  int maxlayer=-1;
  int nlayer=0,tnode=0,tnodey=0,layerbuff=-1;
  String nname="";
  int ltype=0,lfrom=-1,lto=-1,fnodes=-1,fnodee=-1,tnodes=-1,tnodee=-1;
  float vala=NrPop.ra,valb=NrPop.rb;
  int blayer=-1,bnodes=-1,bnodee=-1;
  float bvaluea=0,bvalueb=0;

  for (i=s;i<descrrec.length;i++)
  {
    tok=new StringTokenizer(descrrec[i]);
    if (tok.countTokens()<1) continue;
    t=tok.nextToken();
    if (t.startsWith("layer="))
       {
        val=t.substring(t.indexOf('='));a=NrPop.values(val,1);
        try {nlayer=Integer.parseInt(a);} catch (NumberFormatException e){NNError.err("Layer Number missed at rec "+i);}
        if (nlayer>maxlayer) maxlayer=nlayer;
       }
    if (t.startsWith("net=")) break;
  }
  lyr= new Layer[maxlayer+1];

  for (i=s;i<descrrec.length;i++)
  {
    tok=new StringTokenizer(descrrec[i]);
    if (tok.countTokens()<1) continue;
    t=tok.nextToken();
    if (t.startsWith("layer="))
       {ct=tok.countTokens();
        val=t.substring(t.indexOf('='));a=NrPop.values(val,1);
        try {nlayer=Integer.parseInt(a);} catch (NumberFormatException e){NNError.err("Layer Number missed at rec "+i);}
        for (k=1;k<=ct;k++)
        {t=tok.nextToken();
         if (t.startsWith("tnode="))
            {val=t.substring(t.indexOf('='));a=NrPop.values(val,1);b=NrPop.values(val,2);
            try {tnode=Integer.parseInt(a);} catch (NumberFormatException e){NNError.err("O node total at rec "+i);}
            try {tnodey=Integer.parseInt(b);} catch (NumberFormatException e){tnodey=0;}
            continue;}
         if (t.startsWith("nname="))
            {val=t.substring(t.indexOf('='));a=NrPop.values(val,1);nname=a;
            continue;}
        }
        if (nname.length()==0 && nlayer==0) nname=Pack+"NodeLin";
        if (nname.length()==0 && nlayer!=0) nname=Pack+"NodeSigm";
        if (tnodey==0) lyr[nlayer]= new Layer(nlayer,tnode,nname);
        else lyr[nlayer]= new Layer(nlayer,tnode,tnodey,nname);
       }
    if (t.startsWith("net=")) break;
  }

  for (i=s;i<descrrec.length;i++)
  {
    tok=new StringTokenizer(descrrec[i]);
    if (tok.countTokens()<1) continue;
    t=tok.nextToken();
    if (t.startsWith("layer="))
       {ct=tok.countTokens();
        val=t.substring(t.indexOf('='));a=NrPop.values(val,1);
        try {nlayer=Integer.parseInt(a);} catch (NumberFormatException e){NNError.err("Layer Number missed at rec "+i);}
        /* for (k=1;k<=ct;k++)
        { t=tok.nextToken(); 
         if (t.startsWith("copytoml="))
            {val=t.substring(t.indexOf('='));a=NrPop.values(val,1);
            try {layerbuff=Integer.parseInt(a);} catch (NumberFormatException e){NNError.err("No layer memory number at rec "+i);}
            continue;}
        } */
        if (layerbuff>0 && layerbuff<=maxlayer) lyr[nlayer].mbuff=lyr[layerbuff];
       }
    if (t.startsWith("net=")) break;
  }

  for (i=s;i<descrrec.length;i++)
  {
    tok=new StringTokenizer(descrrec[i]);
    if (tok.countTokens()<1) continue;
    t=tok.nextToken();
    if (t.startsWith("linktype="))
      {ct=tok.countTokens();
       val=t.substring(t.indexOf('='));a=NrPop.values(val,1);if (a.compareToIgnoreCase("one")==0) ltype=1; else ltype=0;
       for (k=1;k<=ct;k++)
       {t=tok.nextToken();
        if (t.startsWith("fromlayer="))
           {val=t.substring(t.indexOf('='));a=NrPop.values(val,1);b=NrPop.values(val,2);c=NrPop.values(val,3);
           try {lfrom=Integer.parseInt(a);} catch (NumberFormatException e){NNError.err("Layer Number missed at rec "+i);}
           try {fnodes=Integer.parseInt(b);} catch (NumberFormatException e){fnodes=0;}
           try {fnodee=Integer.parseInt(c);} catch (NumberFormatException e){fnodee=lyr[lfrom].node.length-1;}
           continue;}
        if (t.startsWith("tolayer="))
           {val=t.substring(t.indexOf('='));a=NrPop.values(val,1);b=NrPop.values(val,2);c=NrPop.values(val,3);
           try {lto=Integer.parseInt(a);} catch (NumberFormatException e){NNError.err("Layer Number missed at rec "+i);}
           try {tnodes=Integer.parseInt(b);} catch (NumberFormatException e){tnodes=0;}
           try {tnodee=Integer.parseInt(c);} catch (NumberFormatException e){tnodee=lyr[lto].node.length-1;}
           continue;}
        if (t.startsWith("value="))
           {val=t.substring(t.indexOf('='));a=NrPop.values(val,1);b=NrPop.values(val,2);
           try {vala=Float.parseFloat(a);} catch (NumberFormatException e){vala=NrPop.ra;valb=NrPop.rb;continue;}
           try {valb=Float.parseFloat(b);} catch (NumberFormatException e){valb=vala;}
           continue;}
       }
       if (lfrom>=0 && lfrom<=maxlayer && lto>=0 && lto<=maxlayer)
       {
         if (ltype==1) lyr[lto].linkOneToOne(tnodes,tnodee-tnodes+1,fnodes,lyr[lfrom],vala,valb);
         if (ltype==0) lyr[lto].linkFromAll(tnodes,tnodee-tnodes+1,fnodes,fnodee-fnodes+1,lyr[lfrom],vala,valb);
       }
      }
    if (t.startsWith("biasval="))
      {ct=tok.countTokens();
       val=t.substring(t.indexOf('='));a=NrPop.values(val,1); b=NrPop.values(val,2);
       try {bvaluea=Float.parseFloat(a);} catch (NumberFormatException e){bvaluea=NrPop.ra;bvalueb=NrPop.rb;continue;}
       try {bvalueb=Float.parseFloat(b);} catch (NumberFormatException e){bvalueb=bvaluea;}
       for (k=1;k<=ct;k++)
       {t=tok.nextToken();
        if (t.startsWith("oflayer="))
           {val=t.substring(t.indexOf('='));a=NrPop.values(val,1);b=NrPop.values(val,2);c=NrPop.values(val,3);
           try {blayer=Integer.parseInt(a);} catch (NumberFormatException e){NNError.err("Layer Number missed at rec "+i);}
           try {bnodes=Integer.parseInt(b);} catch (NumberFormatException e){bnodes=0;}
           try {bnodee=Integer.parseInt(c);} catch (NumberFormatException e){bnodee=lyr[blayer].node.length-1;}
           continue;}
       }
       if (blayer>=0 && blayer<=maxlayer)
       {lyr[blayer].setBias(bnodes,bnodee-bnodes+1,bvaluea,bvalueb);}
      }
    if (t.startsWith("net=")) break;
  }

 }

 /**
 * Return the total number of layers in the net
 */
 public int getLdim(){return lyr.length;}

 /**
 * Returns the total number of nodes in the layer <TT>"layer"</TT>
 */
 public int getLnodes(int layer){return lyr[layer].node.length;}

 /**
 * Returns the bias value of node <TT>"node"</TT> of layer <TT>"layer"</TT>
 */
 public float getLNBias(int layer, int node){return lyr[layer].getBias(node);}

 /**
 * Sets the bias value of node <TT>"node"</TT> of layer <TT>"layer"</TT>
 */
 public void setLNBias(int layer, int node, float bias){lyr[layer].setBias(node,bias);}

 /**
 * Sets the input values of all nodes starting from node <TT>"sn"</TT>,
 * for total <TT>"len"</TT> nodes, on layer <TT>"layer"</TT>
 */
 public void setLInp(int layer,int sn, int len,float inp[]){lyr[layer].inp(sn,len,inp);}
 /**
 * Sets the input value of node <TT>"node"</TT> of layer <TT>"layer"</TT>
 */
 public void setLInp(int layer,int node,float inp){lyr[layer].inp(node,inp);}

 /**
 * Sets the expected output values of all nodes starting from node <TT>"sn"</TT>,
 * for total <TT>"len"</TT> nodes, on layer <TT>"layer"</TT>
 * and returns the squared average error from output computed values and expected values.
 */
 public float setLOexp(int layer,int sn,int len,float oexp[]){return lyr[layer].err(sn,len,oexp);}

 /**
 * Sets the output expected value of node <TT>"node"</TT> of layer <TT>"layer"</TT>
 * and returns the squared error from output computed value and expected value.
 */
 public float setLOexp(int layer,int node,float oexp){return lyr[layer].err(node,oexp);}

 /**
 * Gets (in <TT>"out[]"</TT> array) the output computed values of all nodes
 * starting from node <TT>"sn"</TT>,
 * for total <TT>"len"</TT> nodes, on layer <TT>"layer"</TT>
 */
 public void getLOut(int layer,int sn,int len,float out[]){lyr[layer].out(sn,len,out);}

 /**
 * Returns the output computed value of node <TT>"node"</TT> of layer <TT>"layer"</TT>
 */
 public float getLOut(int layer,int node,float out){return lyr[layer].out(node);}

 /**
 * This method computes the net. From layer 0  to last layer
 */
 public void frwNNet()
 {int i; for (i=0;i<lyr.length;i++) lyr[i].frwLayer();}

 /**
 * This method compute the net. After having loaded the first layer nodes with
 * <TT>"inp[]"</TT> values into inp node variables. Compute the net from layer 0  to last layer
 */
 public void frwNNet(float inp[])
 {int i; int ni;
  if (inp.length<lyr[0].node.length) ni=inp.length; else ni=lyr[0].node.length;
  lyr[0].inp(0,ni,inp);
  for (i=0;i<lyr.length;i++) lyr[i].frwLayer();
 }

 /**
 * This method compute the net. Formely it loads the first layer nodes with <TT>"inp[]"</TT>
 * values into inp node variables,  afterward it computes the net from layer 0
 * to last layer and at last it returns the output values of last layer into the <TT>"out[]"</TT> array.
 */
 public void frwNNet(float inp[],float out[])
 {int i; int ni; int no;
  if (inp.length<lyr[0].node.length) ni=inp.length; else ni=lyr[0].node.length;
  if (out.length<lyr[lyr.length-1].node.length) no=inp.length;
  else no=lyr[lyr.length-1].node.length;
  lyr[0].inp(0,ni,inp);
  for (i=0;i<lyr.length;i++) lyr[i].frwLayer();
  lyr[lyr.length-1].out(0,no,out);
 }

 /**
 * This method test the net. It applies <TT>"inp[]"</TT> values and compute the net.
 * At last compare the output value of last layer with expected values <TT>"expout[]"</TT>
 * and return the average squared error.
 */
 public float testNNet(float inp[],float outexp[])
 {
  int i,nout,no;
  float diff,adiff,errqm=0;
  nout=lyr[lyr.length-1].node.length;
  float out[]= new float[nout];
  if (outexp.length<nout) no=outexp.length;  else no=nout;
  frwNNet(inp,out);
  for (i=0;i<no;i++) {diff=outexp[i]-out[i]; errqm=errqm+diff*diff;}
  errqm=errqm/no;
  return errqm;
 }

 NTestRet testStructNNet(float inp[],float outexp[])
 {
  int i,nout,no;
  float diff,adiff;
  NTestRet ret=new NTestRet();
  nout=lyr[lyr.length-1].node.length;
  float out[]= new float[nout];
  if (outexp.length<nout) no=outexp.length;
  else no=nout;
  frwNNet(inp,out);
  for (i=0;i<no;i++)
  {
   diff=outexp[i]-out[i];
   adiff=Math.abs(diff);
   ret.errm=ret.errm+diff;
   ret.errqm=ret.errqm+diff*diff;
   if (adiff<ret.minerr) ret.minerr=adiff;
   if (adiff>ret.maxerr) ret.maxerr=adiff;
   if (adiff>0.5) ret.digiterr++;
  }
  ret.errm=ret.errm/no;
  ret.errqm=ret.errqm/no;
  ret.dist=(float)(Math.sqrt((double)ret.errqm)/Math.sqrt((double)no));
  return ret;
 }

 /**
 * This method test the net using a list of coupled values input-outputexpetted.
 * This list is supplied by a string array (each element is a couple inp-out).
 * It returns the average squared error (for the whole list)
 */
 public float testNNet(String train[])
 {
   int i,r;
   float errqm=0;
   String t;
   if (train.length<1) return errqm;
   if (lyr.length<1) return errqm;
   int ol=lyr.length-1;
   int ninp=lyr[0].node.length, nout=lyr[ol].node.length;
   float inp[]=new float[ninp];
   float oexp[]=new float[nout];
   for (r=0;r<train.length;r++)
   {
    StringTokenizer tok=new StringTokenizer(train[r]);
    try
    {
     for (i=0;i<ninp;i++)
        {t=tok.nextToken();inp[i]=Float.parseFloat(t);}
     for (i=0;i<nout;i++)
        {t=tok.nextToken();oexp[i]=Float.parseFloat(t);}
    }
    catch (NumberFormatException e){NNError.err("Missmatch value in train record "+r);}
    errqm=errqm+testNNet(inp,oexp);
   }
   return errqm/train.length;
 }

 /**
 * Simplified version of outNNet method. Net as a filter.
 */
 public void NNetFilter(String fileinp, String fileout)
  {outNNet(fileinp,fileout,false,false,false,false);}

 /**
 * Simplified version of outNNet method. It verifies NET and displays values inp,out and
 * expetted out on console.
 */
 public void verifyNNet(String fileinp)
  {outNNet(fileinp,"",false,false,true,false);}

 /**
 * Simplified version of outNNet method. It traines Net by a stream. Useful in case
 * of long examples set or continous training.
 */
 public void trainNNet(String fileinp,String fileout)
  {outNNet(fileinp,fileout,true,false,false,true);}

 /**
 * This method uses the net as a black box. Read a file as input and write a file
 * as output. Output file is the net output for each read input.<p>
 * Flags means:<BR>
 * ftrain : if true the input record contains the expected value and the net is
 * trained using EBP algorithm<BR>
 * inpout : if true both, input values and output values, are wroten on output.<BR>
 * inpoutoexp : if true 3 values: input, output and values expected, are wroten on output.<BR>
 * err  : if true error is wroten on console
 */
 public void outNNet(String fileinp,String fileout,boolean ftrain,boolean inpout,boolean inpoutoexp,boolean err)
 {
   int i,r;
   float errqm=0;
   String t;
   if (lyr.length<1) return ;
   int ol=lyr.length-1;
   int ninp=lyr[0].node.length, nout=lyr[ol].node.length;
   float inp[]=new float[ninp];
   float out[]=new float[nout];
   float oexp[]=new float[nout];
   String line;
   FileReader fread;
   LineNumberReader fline;
   FileOutputStream fout;
   PrintStream fprint;
   try
   {
    if (fileinp.length()==0) fread=new FileReader(FileDescriptor.in);
    else fread= new FileReader(fileinp);
    fline= new LineNumberReader(fread);
    if (fileout.length()==0) fout=new FileOutputStream(FileDescriptor.out);
    else fout= new FileOutputStream(fileout);
    fprint=new PrintStream(fout);
    while(true)
    { try {line=fline.readLine();
           if (line==null) break;
           StringTokenizer tok=new StringTokenizer(line);
           try {for (i=0;i<ninp;i++)
               {t=tok.nextToken();inp[i]=Float.parseFloat(t);}}
           catch (NumberFormatException e){NNError.err("Missmatch value in input record ");}
           try {i=0;while(tok.hasMoreTokens())
                   {if(i>nout-1) break; t=tok.nextToken();oexp[i]=Float.parseFloat(t);i++;}}
           catch (NumberFormatException e){if (ftrain) NNError.err("Missmatch value in train record ");}
           if (ftrain) errqm=ebplearnNNet(inp,out,oexp);
           else frwNNet(inp,out);
           if (inpout | inpoutoexp)
              {for (i=0;i<ninp;i++) fprint.print(inp[i]+" ");fprint.print("    ");}
           if (inpoutoexp)
              {for (i=0;i<nout;i++) fprint.print(oexp[i]+" ");fprint.print("    ");}
           for (i=0;i<nout;i++) fprint.print(out[i]+" ");fprint.println("");
           if (err) System.out.println(errqm);
          }
       catch (IOException e) {break;} }
    try{if (fileinp.length()!=0) {fline.close();fread.close();}
        if (fileout.length()!=0) {fprint.close();fout.close();}}
    catch (IOException e){};
   }catch (FileNotFoundException e) {NNError.err("File not found ");}
 }


 /**
 * Error Back Propagation (EBP) phase. Weights modification starting from last
 * layer to firts layer (using the error values suplied on last layer nodes.<BR>
 * If <TT>"fbias"</TT> is true bias is trained.
 * <TT>"eps"</TT> is the learning factor and alfa the momentum factor
 */
 public void ebpNNet(boolean fbias,float eps,float alfa)
 {int i; for (i=0;i<lyr.length;i++) lyr[i].ebpLayer(fbias,eps,alfa);}

 /**
 * Error Back Propagation (EBP) phase. Weights modification starting from last
 * layer to firts layer (using the error values suplied on last layer nodes.<BR>
 * Default values as parameters (see NrPop for default)
 */
 public void ebpNNet()
 {ebpNNet(NrPop.fbias,NrPop.eps,NrPop.alfa);}

 /**
 * Error Back Propagation (EBP) phase with out-expected values suplied.
 */
 public float ebpNNet(float outexp[],boolean fbias,float eps,float alfa)
 {int i; int no; float errq;
  if (outexp.length<lyr[lyr.length-1].node.length) no=outexp.length;
  else no=lyr[lyr.length-1].node.length;
  errq=lyr[lyr.length-1].err(0,no,outexp);
  for (i=lyr.length-1;i>=0;i--) lyr[i].ebpLayer(fbias,eps,alfa);
  return errq;
 }

 /**
 * Error Back Propagation (EBP) phase with out-expected values suplied.
 * Default parameters
 */
 public float ebpNNet(float outexp[])
 {return ebpNNet(outexp,NrPop.fbias,NrPop.eps,NrPop.alfa);}

 /**
 * Error Back Propagation (EBP) phase with a couple of inp and out-expected
 * values suplied. Default parameters
 */
 public float ebplearnNNet(float inp[],float outexp[])
 {int i; int ni; int no;
  if (outexp.length<lyr[lyr.length-1].node.length) no=inp.length;
  else no=lyr[lyr.length-1].node.length;
  frwNNet(inp);
  return ebpNNet(outexp);
 }

 /**
 * Same as previous but also returns output values in <TT>"out[]"</TT> buffer.
 */
 public float ebplearnNNet(float inp[],float out[],float outexp[])
 {int i; int ni; int no;
  if (outexp.length<lyr[lyr.length-1].node.length) no=inp.length;
  else no=lyr[lyr.length-1].node.length;
  frwNNet(inp,out);
  return ebpNNet(outexp);
 }

 /**
 * Main EBP learning method. A string array <TT>"train[]"</TT> musts contain a list
 * of coupled inp-outexp values (a couple in each record).
 * <PRE>
 * Example: (XOR problem)
 * 0 0   0
 * 1 0   1
 * 0 1   1
 * 1 1   0
 *
 * (spaces are not inportant)
 *</PRE>
 */
 public float ebplearnNNet(String train[])
 {
   int i,r;
   float errqm=0;
   String t;
   if (train.length<1) return errqm;
   if (lyr.length<1) return errqm;
   int ol=lyr.length-1;
   int ninp=lyr[0].node.length, nout=lyr[ol].node.length;
   float inp[]=new float[ninp];
   float oexp[]=new float[nout];
   for (r=0;r<train.length;r++)
   {
    StringTokenizer tok=new StringTokenizer(train[r]);
    try
    {
     for (i=0;i<ninp;i++)
        {t=tok.nextToken();inp[i]=Float.parseFloat(t);}
     for (i=0;i<nout;i++)
        {t=tok.nextToken();oexp[i]=Float.parseFloat(t);}
    }
    catch (NumberFormatException e){NNError.err("Missmatch value in train record "+r);}
    frwNNet(inp);
    errqm=errqm+ebpNNet(oexp);
   }
   return errqm/train.length;
 }

 /**
 * Same as other ebplearn method, but use a file instead of a string array
 * Usefull for short repeated training set. For continous stream see trainNNet.
 */
 public float ebplearnNNet(String trainfile)
 {
    String train[]=NrPop.fileToStrArray(trainfile);
    if (train!=null)  return ebplearnNNet(train);
    else return 0;
 }

 /**
 * Return a NNet object with the same structure and weights of this net
 */
 public NNet cloneNNet()
 {
  int i,n,l,lb;
  NNet newnet;
  Node nfrom;
  newnet=new NNet(lyr.length);
  for (i=0;i<lyr.length;i++) newnet.lyr[i]=lyr[i].cloneLayer();
  for (i=0;i<lyr.length;i++)
      {lb=lyr[i].getnlBuff(); if (lb>=0) newnet.lyr[i].mbuff=newnet.lyr[lb];}
  for (i=0;i<lyr.length;i++) lyr[i].cloneLink(newnet);
  return newnet;
 }

 /**
 * This method copies weights from this network in another <TT>"tonet"</TT> net.
 * Copiing is sequential from layer 0 to last layer. If the two nets have not
 * the same structure, result is unpredictable, but there isn't error, because
 * the smalest recipe guides copiing.
 */
 public void copyWNNet(NNet tonet)
 {
  int i,nl;
  if (tonet.lyr.length<lyr.length) nl=tonet.lyr.length;
  else nl=lyr.length;
  for (i=0;i<nl;i++) lyr[i].copyWLayer(tonet.lyr[i]);
 }

 /**
 * This method initialise all weights with random values.
 * <TT>"ra"</TT> and <TT>"rb"</TT> define range of linear distributed random value.
 * If <TT>"fbias"</TT> is true  bias  is random initialise to.
 */
 public void inirandNNet(float ra, float rb,boolean fbias)
 {
   int i,n,l;
   for (i=0;i<lyr.length;i++) lyr[i].inirandLayer(ra,rb,fbias);
 }

 /**
 * This method applies a random mutation (with linear distribution)on net weights.
 * <TT>"ra"</TT> and <TT>"rb"</TT> define range of linear distributed random value added
 * If <TT>"fbias"</TT> is true  bias  is modified.
 */
 public void wgtmutNNet(float ra, float rb,boolean fbias)
 {
   int i,n,l;
   for (i=0;i<lyr.length;i++) lyr[i].wgtmutLayer(ra,rb,fbias);
 }

 /**
 * This method applies a random mutation (with Gaussian distribution)on net weights.
 * <TT>"dev"</TT> id standard deviation (alf of width at 1/e (0.36) of peak)
 * If <TT>"fbias"</TT> is true  bias  is modified.
 */
 public void wgtmutNNet(float dev,boolean fbias)
 {
   int i,n,l;
   for (i=0;i<lyr.length;i++) lyr[i].wgtmutLayer(dev,fbias);
 }

 /**
 * This method save the net on a string.
 * Net can be read with NNet(String namestring,boolean load,boolean stringsource)
 * constructor.
 * <PRE>
 * Format:
 * NET "number" "total-number-of-layers"
 * "num-of-this-layer"  "tot-nodes-x-dim"  "tot-nodes-y-dim"  "name-of-class-node" "layer-buffer"or -1
 * "num-of-this-node"  "tot-links-from"  "bias"
 * "num-layer-from","num-node-from":"weight"  "...,...:..."  .......
 * (other node)
 *  ...
 * (other layer)
 *  ...
 * Example: (net 2-2-1 for XOR problem)
 * NET 1 3
 * 0 2 0 NodeLin -1
 * 0 0 0
 * 0 1 0
 * 1 2 0 NodeSigm -1
 * 0 2 0
 * 0,0:0.44 0,1:-0.8324
 * 1 2 0
 * 0,0:0.003 0,1:0.775
 * 2 1 0 NodeSigm -1
 * 0 2 0
 * 1,0:-0.567  1,1:0.66
 * </PRE>
 */
 public String toString()
 {
   int i;
   ByteArrayOutputStream fileout;
   PrintStream pf;
//   try
   {
    fileout= new ByteArrayOutputStream();
    pf=new PrintStream(fileout);
    this.saveNNet(pf,1);
    pf.close();
    return fileout.toString();
   }
//   catch (FileNotFoundException e){NNError.err("Impossible to create string ");return null;}
 }


 /**
 * This method save the net on file named <TT>"nomefile"</TT>. (see toString for format)
 * Net can be read with NNet(String namefiles,boolean load) constructor
 */
 public void saveNNet(String nomefile, AdvancedRobot bot)
 {
   int i;
   RobocodeFileOutputStream fileout;
   PrintStream pf;
   try
   {
    //fileout= new FileOutputStream(nomefile);
	ZipOutputStream zipout = new ZipOutputStream(new RobocodeFileOutputStream(bot.getDataFile(nomefile + ".zip")));
	zipout.setLevel(9);
	zipout.putNextEntry(new ZipEntry(nomefile));
	//fileout = new RobocodeFileOutputStream(bot.getDataFile(nomefile));
    //pf=new PrintStream(fileout);
	pf=new PrintStream(zipout);
    this.saveNNet(pf,1);
	zipout.flush(); zipout.close();
	pf.close(); 
   }
   catch (Exception e){NNError.err("Impossible to create file "+nomefile); }
 }

 /**
 * This method save the net on a file already open.<TT>"n"</TT> is a number that
 * identify the net.
 */
 public void saveNNet(PrintStream pf,int n)
 {
   int i;
   pf.println(" ");
   pf.println("NET "+n+" "+this.lyr.length);
   for (i=0;i<lyr.length;i++) this.lyr[i].saveLayer(pf);
   pf.println(" ");
   for (i=0;i<lyr.length;i++) {this.lyr[i].saveLink(pf);pf.println(" ");}
   pf.flush();
 }

 /**
 * This constructor loads a net from a file named <TT>"namefile"</TT>.
 * The boolean parameter is there just for distinguishing from new net constructor
 */
 public NNet(String namefile,boolean load, AdvancedRobot bot) throws java.lang.Exception
 {
   int i;
   int npop=0,net;
   FileInputStream fileinp;
   InputStreamReader fileread;
   StreamTokenizer tok;
   try
   {
	ZipInputStream zipin = new ZipInputStream(new FileInputStream(bot.getDataFile(namefile + ".zip")));
	zipin.getNextEntry();
			
	//fileinp= new FileInputStream(bot.getDataFile(namefile));
    //fileread= new InputStreamReader(fileinp);
	
	fileread= new InputStreamReader(zipin);

    tok=new StreamTokenizer(fileread);
    tok.whitespaceChars(0x2C,0x2C); /* "," */
    tok.whitespaceChars(0x3A,0x3A); /* ":" */
    try{tok.nextToken();}catch (IOException e){return;}
    if (tok.sval.equals("NET"))
    try{tok.nextToken(); net=(int)tok.nval;}catch (IOException e){NNError.err("Error on net file");return;}
    this.loadNNet(tok);

   }
   catch (FileNotFoundException e){ throw new java.lang.Exception(); }
 }

 /**
 * This constructor loads a net from the string <TT>"namestring"</TT>.
 * The boolean parameter stringsource is there just for distinguishing from
 * similar constructor (that loads from file)
 */
 public NNet(String namestring,boolean load,boolean stringsource)
 {
   int i;
   int npop=0,net;
   StringReader fileread;
   StreamTokenizer tok;
//   try
   {
    fileread= new StringReader(namestring);
    tok=new StreamTokenizer(fileread);
    tok.whitespaceChars(0x2C,0x2C); /* "," */
    tok.whitespaceChars(0x3A,0x3A); /* ":" */
    try{tok.nextToken();}catch (IOException e){return;}
    if (tok.sval.equals("NET"))
    try{tok.nextToken(); net=(int)tok.nval;}catch (IOException e){NNError.err("Error on net file");return;}
    this.loadNNet(tok);
//    fileread.close();
   }
//   catch (FileNotFoundException e){NNError.err("String not found "+namestring);}
 }


 void loadNNet(StreamTokenizer tok)
 {
   int i,nlayer;
   try {tok.nextToken(); nlayer=(int)tok.nval;}catch (IOException e){NNError.err("Error on net file");return;}
   this.lyr=new Layer[nlayer];
   for (i=0;i<this.lyr.length;i++) this.lyr[i]=loadLayer(tok,this.lyr);
   for (i=0;i<this.lyr.length;i++) this.lyr[i].loadLink(tok,this.lyr);
 }

 Layer loadLayer(StreamTokenizer tok, Layer layer[])
 {
   int nlyr,nx,ny,lbuff;
   String Cnode;
   Layer lay;
   try
   {
   tok.nextToken(); nlyr=(int)tok.nval;
   tok.nextToken(); nx=(int)tok.nval;
   tok.nextToken(); ny=(int)tok.nval;
   tok.nextToken(); Cnode=tok.sval;
   tok.nextToken(); lbuff=(int)tok.nval;
   }catch (IOException e){ NNError.err("Error on net file");return null;}
   if (ny>0) lay=new Layer(nlyr,nx,ny,Cnode);
   else lay=new Layer(nlyr,nx,Cnode);
   if (lbuff>=0) lay.mbuff=layer[lbuff];
   return lay;
 }


}

/************************************************************************/


/************************************************************************/

class NTestRet
{
 float errqm;
 float maxerr=0;
 float minerr=(float)9999999.0;
 float errm;
 float digiterr;
 float dist;
}

/************************************************************************/
