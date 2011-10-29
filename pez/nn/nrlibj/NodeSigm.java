package pez.nn.nrlibj;



/**
* Class NodeSigm extend NodeLin utilising a sigmoid function of activation instead
* of liner function.
* <PRE>
*
*             inp-> /-----------------\
*      link-------->|       |         |
*      link-------->| sum   | afn(sum)|
*      link-------->|       | df(sum) |
*          bias---->\-----------------/
*
*
* In NodeSigm: afn(float x) {return 1/(1+exp(-x));}
*              df(float x) {return 1;}
*</PRE>
* @author D. Denaro
* @version 5.0 , 2/2001
*/
public class NodeSigm extends NodeLin
{
 public NodeSigm(int nn,int nly,float b){super(nn,nly,b);};
 protected float afn(float x) {return (float)(1/(1+Math.exp((double)-x)));}
 protected float df(float y) {return (float)y*(1-y);}
}


/************************************************************************/
