package pez.nn.nrlibj;


/**
* Class NodeLin (see it) realize the base Class for classical feed-forward neural
* network (and can use EBP learning). In NodeLin Class, trf() function is split
* in activation routine (sum of link) and activation function afn(). Also ebp
* function is split in ebp procedure and derivative of activation function.
* So, others kind of nodes that use the classical node paradigm (sum of link),
* but differ for activation function have to extend this NodeLin Class redefining
* just afn(x)and its derivative df(x).Class NodeSigm, for example redefine afn()
* as sigmoid function and df() as its derivative.
* <PRE>
*
*             inp-> /-----------------\
*      link-------->|       |         |
*      link-------->| sum   | afn(sum)|
*      link-------->|       | df(sum) |
*          bias---->\-----------------/
*
*
* In NodeLin: afn(float x) {return x;}
*             df(float x) {return 1;}
* </PRE>
* This kind of node is useful in input layer (the first layer)
* @author D. Denaro
* @version 5.0 , 2/2001
*/
public class NodeLin extends Node
{
 public NodeLin(int nn,int nly,float b){super(nn,nly,b);};
 protected float afn(float act) {return act;}
 protected void trf()
  {
   int i;
   float act;
   act=inp+lnk[0].wgt;
   for (i=1;i<lnk.length;i++){ act=act+lnk[i].nfrom.out*lnk[i].wgt;}
   outb=afn(act);
  }
 protected float df(float y){return (float)1;}
 protected void ebp(boolean fbias, float eps, float alfa)
 {
  int i;
  float dw,derr;
  derr=err*df(out);
  if (fbias)
  {dw=-eps*derr;dw=dw+alfa*lnk[0].wgtb; lnk[0].wgtb=dw;
   lnk[0].wgt=lnk[0].wgt+dw;}
  for (i=1;i<lnk.length;i++)
  { lnk[i].nfrom.errb=lnk[i].nfrom.errb+derr*lnk[i].wgt;
    dw=-eps*derr*lnk[i].nfrom.out; dw=dw+alfa*lnk[i].wgtb; lnk[i].wgtb=dw;
    lnk[i].wgt=lnk[i].wgt+dw;}
 }
}
