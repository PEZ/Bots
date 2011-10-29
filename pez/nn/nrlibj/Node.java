package pez.nn.nrlibj;

/************************************************************************/
/*                                                                      */
/*                                                                      */
/*                 CLASS  Node                                          */
/*                                                                      */
/*                                                                      */
/************************************************************************/

/**
* This Class is the abstract Class that every node have to extend.
* Node is the computational unit of Neural Network.<BR>
* This abstract class is general; it contain the input, output and error variable,
* the links from other nodes and two methods used by forward phase and EBP phase
* to sincronise layer computation.<p>
* Customer node have to implement :<BR>
*  trf() function : a function that is computed in forward phase<BR>
*  ebp(fbias,eps,alfa) function : a function that is computed in EBP phase<BR>
* <BR>
* Of course, these function can be empty or very complicated.<BR>
* For example the NodeLin class implement trf() as a sum of incoming nodes output
* (sum of products: outputs*weights) usualy named activation.<BR>
* But it is possible to use any other function that can use these node variables:
* <PRE>
*  inp                    : is an external input (or charged by buffered layers)
*  lnk[0].wgt             : bias value
*  lnk[i>0].nodefrom.out  : output of node linked to this
*  lnk[i>0].wgt           : weight of this link
*  outb                   : output for next layer or for external use
*
*  for 2 dimension layer only:
*  ngb[4]     : neightbour nodes
* </PRE>
* If you want use EBP phase, ebp function have to realize the error back propagation
* and the weights modification for all links to this node.<BR>
* <BR>
* Class NodeLin (see it) realize the base Class for classical feed-forward neural
* network (and can use EBP learning). In NodeLin Class, trf() function is split
* in activation routine (sum of link) and activation function afn(). Also ebp
* function is split in ebp procedure and derivative of activation function.
* So, others kind of nodes that use the classical node paradigm (sum of link),
* but differ for activation function have tu extend this NodeLin Class redefining
* just afn() and its derivative df(). Class NodeSigm, for example redefine afn()
* as sigmoid function and df() as its derivative.<p>
*
* Others kind of nodes that want to combine linked nodes in different way
* (for example Radial Basis Function) have to extend directly this abstract
* class and define its trf().<p>
*
* But a node class can also directly produce external actions.In this case you
* can use your trf() or override method out().
* @author D. Denaro
* @version 5.0 , 2/2001
*/
public abstract class Node
{
  protected int nn;
  protected int nlyr;
  protected float inp;
  protected float outb;
  protected float out;
 abstract protected void trf();
  protected void out(){out=outb;outb=0;}
  protected float err;
  protected float errb;
  protected Link lnk[];
  protected Node ngb[];
 abstract protected void ebp(boolean fbias,float eps,float alfa);
  protected void err(){err=errb;errb=0;}
 public Node(int nn,int nlyr,float bias)
 {this.nn=nn;this.nlyr=nlyr; lnk=new Link[1]; lnk[0]=new Link(bias,null); ngb=null;}
 void nodelink(int s, int l,Layer lfrom,float wgt[])
 {int i,oldl; Link newlnk[];oldl=lnk.length;
  newlnk= new Link[l+oldl];System.arraycopy(lnk,0,newlnk,0,oldl);
  lnk=newlnk;
  for (i=0;i<l;i++) {lnk[i+oldl]=new Link(wgt[i],lfrom.node[i+s]);}
 }
 protected float Wgt(int i){return lnk[i].wgt;}
 protected float Wgtb(int i){return lnk[i].wgtb;}
 protected void sWgt(int i,float w){lnk[i].wgt=w;}
 protected void sWgtb(int i,float w){lnk[i].wgtb=w;}
 protected Node  Nfrom(int i){return lnk[i].nfrom;}
 protected float outNfrom(int i){return lnk[i].nfrom.out;}
 protected float errbNfrom(int i){return lnk[i].nfrom.errb;}
 protected void serrbNfrom(int i,float e){lnk[i].nfrom.errb=e;}
 protected int   Nnumfrom(int i){return lnk[i].nfrom.nn;}
 protected int   Lnumfrom(int i){return lnk[i].nfrom.nlyr;}
 protected Node  Nneigh(int i){return ngb[i];}
 protected int Nn(){return nn;}
 protected int Nl(){return nlyr;}
 protected float Out(){return out;}
 protected void sInp(float in){inp=in;}
}

/************************************************************************/
