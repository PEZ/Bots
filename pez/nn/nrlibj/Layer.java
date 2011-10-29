package pez.nn.nrlibj;

import java.io.*;
import java.lang.*;
import java.lang.reflect.*;
import java.util.*;

class Layer
{
 int nlyr;
 int nx;
 int ny;
 Layer mbuff;
 Node node[];

 Layer(int nlyr,int nx,int ny, Layer mbuff)
 {
   this.nlyr=nlyr; this.nx=nx; this.ny=ny; this.mbuff=mbuff;
   if (ny==0) this.node= new Node[nx]; else this.node= new Node[nx*ny];
 }

 Layer(int nlyr,int nx,String SCnode)
 {
  int i;
  String NCnode=null,Pack,ThisC;
  Class Cnode=null;
  this.nlyr=nlyr;
  this.nx=nx;
  ny=0;
  ThisC=this.getClass().getName();
  Pack=ThisC.substring(0,ThisC.lastIndexOf('.')+1);
  mbuff=null;
  node= new Node[nx];
  if (nx<1) return;
  //try {Cnode= Class.forName(".\\apv\\nrlibj"+SCnode);}
  /*
  try {Cnode= Class.forName(SCnode);}
  catch (ClassNotFoundException e) {
	System.out.println("Not found in APV"+" "+Pack+" "+SCnode); 
	NCnode=Pack+SCnode;
  }
  */
  try
  {
	/*
  	if (Cnode==null) {Cnode= Class.forName(NCnode);}
  	{if (!Node.class.isAssignableFrom(Cnode)) {NNError.err("No EBP type node");}}
  	String nome= Cnode.toString();
  	Constructor CCnode= Cnode.getConstructor(new Class[] {int.class,int.class,float.class});
  	//for (i=0;i<nx;i++)
  	//node[i]= (Node)CCnode.newInstance(new Object[] {new Integer(i), new Integer (nlyr), new Float(0)});
	*/
	for (i=0;i<nx;i++) {
		if (SCnode.equals("NodeLin")) node[i]= new NodeLin(i,nlyr,0); else node[i]= new NodeSigm(i,nlyr,0);
	}
  }
  catch (Exception e) { System.out.println("ERROR GRAVISIMO"); }
  //catch (IllegalAccessException e) {NNError.err("No Access to Class Node :"+SCnode);}
  //catch (InstantiationException e) {NNError.err("No Instantiation Class Node :"+SCnode);}
  //catch (ClassNotFoundException e) {NNError.err("No Class Node Defined :"+SCnode);}
  //catch (LinkageError e) {}
  //catch (SecurityException e) {}
  //catch (NoSuchMethodException e) {NNError.err("No or invalid constructor :"+SCnode);}
  //catch (InvocationTargetException e) {NNError.err("No Class Node Defined :"+SCnode);}

 }


 Layer(int nlyr,int nx,int ny,String SCnode)
 {
  this(nlyr,nx*ny,SCnode);
  int y;int x;int i;int j;
  this.ny=ny;
  for (y=0;y<ny;y++)
     for (x=0;x<nx;x++) {i=x+y*nx;
                         if (x+1 < nx) {j=x+1+y*nx; node[i].ngb[0]=node[j];};
                         if (y+1 < ny) {j=x+(y+1)*nx; node[i].ngb[0]=node[j];};
                         if (x-1 >  0) {j=x-1+y*nx; node[i].ngb[0]=node[j];};
                         if (y-1 >  0) {j=x+(y-1)*nx; node[i].ngb[0]=node[j];};
                        }
  }


 void linkFromAll(int s,int l,int sfr,int lfr,Layer lfrom,float ra,float rb)
 { int i;int j; float wgt[];
   if (s>node.length-1) return;
   if (sfr>lfrom.node.length-1) return;
   if ((s+l)>node.length) l=node.length-s;
   if ((sfr+lfr)>lfrom.node.length) lfr=lfrom.node.length-sfr;
   wgt= new float[lfr];
   for (i=0;i<l;i++)
   {for (j=0;j<lfr;j++) wgt[j]=NrPop.rnd.fab(ra,rb);
    node[i+s].nodelink(sfr,lfr,lfrom,wgt);}
 }

 void linkFromAll(Layer lfrom,float ra,float rb)
 {linkFromAll(0,node.length,0,lfrom.node.length,lfrom,ra,rb);}

 void linkOneToOne(int s,int l,int sfr,Layer lfrom,float ra,float rb)
 { int i;int j; float wgt[];
   if (s>node.length-1) return;
   if (sfr>lfrom.node.length-1) return;
   if ((s+l)>node.length) l=node.length-s;
   if ((sfr+l)>lfrom.node.length) l=lfrom.node.length-sfr;
   if (l<0) return;
   wgt= new float[1];
   for (i=0;i<l;i++)
   {wgt[0]=NrPop.rnd.fab(ra,rb);
    node[i+s].nodelink(i+s,1,lfrom,wgt);}
 }

 void linkOneToOne(Layer lfrom,float ra,float rb)
 {linkOneToOne(0,node.length,0,lfrom,ra,rb);}

 /*
 void linkNgb(int d,int s,int l,int p,Layer lfrom,float ra,float rb)
 {
  int i;
  if (lfrom.ny=0) return;
  if ((s+l)>lfrom.node.length) l=lfrom.node.length-s;
  for (i=0;i<l;i=i+p)
  {}
 }
*/
  int getnlBuff(){if (mbuff!=null) return mbuff.nlyr;else return -1;}

  void frwLayer()
 {
  int i;
  for (i=0;i<node.length;i++) node[i].trf();
  for (i=0;i<node.length;i++) node[i].out();
  if (mbuff!=null)
     {for (i=0;i<node.length;i++)
      {if (i<mbuff.node.length) mbuff.node[i].inp=mbuff.node[i].inp+node[i].out;}}
 }

  void ebpLayer(boolean bias,float eps,float alfa)
 {
  int i; Node nodee;
   for (i=0;i<node.length;i++) {nodee=node[i];nodee.err();}
   for (i=0;i<node.length;i++) {nodee=node[i];nodee.ebp(bias,eps,alfa);}
 }

  void inp(int nn,float inp)
 { node[nn].inp=inp;}

  void inp(int start,int length,float inp[])
 {
  int i;
  if (length+start>node.length) length=node.length-start;
  if (length>inp.length) length=inp.length;
  for (i=0;i<length;i++) node[i+start].inp=inp[i];
 }

  float out(int nn)
 {return node[nn].out;}

  void out(int start,int length,float out[])
 {
  int i;
  if (length+start>node.length) length=node.length-start;
  if (length>out.length) length=out.length;
  for (i=0;i<length;i++) out[i]=node[i+start].out;
 }

  float err(int nn,float out)
 { node[nn].errb=node[nn].out-out; return node[nn].errb*node[nn].errb;}

  float err(int start,int length,float out[])
 {
  int i;float err, errq=0; Node nodee;
  if (length+start>node.length) length=node.length-start;
  if (length>out.length) length=out.length;
  for (i=0;i<length;i++)
  {nodee=node[i+start];err=nodee.out-out[i];nodee.errb=err; errq=errq+err*err;}
  return errq/length;
 }

  void setBias(int nn,float ba)
 {
  int i;
  if (nn>node.length-1) return;
  node[nn].lnk[0].wgt=ba;
 }

  void setBias(int start,int length,float ba,float bb)
 {
  int i;
  if (start>node.length-1) return;
  if (length+start>node.length) length=node.length-start;
  for (i=0;i<length;i++) node[i+start].lnk[0].wgt=NrPop.rnd.fab(ba,bb);;
 }

  void setAllBias(float ba,float bb)
 {int i;for (i=0;i<node.length;i++) node[i].lnk[0].wgt=NrPop.rnd.fab(ba,bb);}

  float getBias(int nnode) {return node[nnode].lnk[0].wgt;}

 Layer cloneLayer()
 {
  int i,n;
  Layer newlayer;
  String SCnode="";
  if (nx>0) SCnode=node[0].getClass().getName();
  if (ny>0) newlayer=new Layer(nlyr,nx,ny,SCnode);
  else newlayer=new Layer(nlyr,nx,SCnode);
  return newlayer;
 }

  void cloneLink(NNet newnet)
 {
   int n,l,nlfrom,nnfrom;
   Node nfrom;
   float wgt;
   for (n=0;n<node.length;n++)
    {
     newnet.lyr[this.nlyr].node[n].lnk=new Link[this.node[n].lnk.length];
     for (l=0;l<node[n].lnk.length;l++)
     { wgt=node[n].lnk[l].wgt;
       nlfrom=node[n].lnk[l].getLfrom();
       nnfrom=node[n].lnk[l].getNfrom();
       if (nnfrom!=-1) nfrom=newnet.lyr[nlfrom].node[nnfrom];
       else nfrom=null;
       newnet.lyr[this.nlyr].node[n].lnk[l]=new Link(wgt,nfrom);}
    }
 }

  void copyWLayer(Layer tolayer)
 {
  int n,l,nn,ll;
  float wgt;
  if (tolayer.node.length<node.length) nn=tolayer.node.length;
  else nn=node.length;
   for (n=0;n<nn;n++)
    {
     if (tolayer.node[n].lnk.length<node[n].lnk.length) ll=tolayer.node[n].lnk.length;
     else ll=node[n].lnk.length;
     for (l=0;l<ll;l++)
     {
       wgt=node[n].lnk[l].wgt;
       tolayer.node[n].lnk[l].wgt=wgt;}
    }
 }

  void inirandLayer(float ra,float rb,boolean fbias)
 {
  int n,l;
  float wgt;
  for (n=0;n<node.length;n++)
  {  if (fbias){node[n].lnk[0].wgt=NrPop.rnd.fab(ra,rb);}
     for (l=1;l<node[n].lnk.length;l++)
     {node[n].lnk[l].wgt=NrPop.rnd.fab(ra,rb);}
  }
 }

  void wgtmutLayer(float ra,float rb,boolean fbias)
 {
  int n,l;
  float wgt;
  for (n=0;n<node.length;n++)
  {  if (fbias){wgt=node[n].lnk[0].wgt;
                wgt=wgt+NrPop.rnd.fab(ra,rb);
                node[n].lnk[0].wgt=wgt;}
     for (l=1;l<node[n].lnk.length;l++)
     { wgt=node[n].lnk[l].wgt;
       wgt=wgt+NrPop.rnd.fab(ra,rb);
       node[n].lnk[l].wgt=wgt;}
  }
 }

  void wgtmutLayer(float dev,boolean fbias)
 {
  int n,l;
  float wgt;
  for (n=0;n<node.length;n++)
  {  if (fbias){wgt=node[n].lnk[0].wgt;
                wgt=wgt+NrPop.rnd.gauss(dev);
                node[n].lnk[0].wgt=wgt;}
     for (l=0;l<node[n].lnk.length;l++)
     { wgt=node[n].lnk[l].wgt;
       wgt=wgt+NrPop.rnd.gauss(dev);
       node[n].lnk[l].wgt=wgt;}
  }
 }

  void saveLayer(PrintStream pf)
 {
  int i,x,y,n,mbuff=-1;
  String SClass;
  if (this.mbuff != null) mbuff=this.mbuff.nlyr;
  if (this.node.length>0)
  {
   SClass=this.node[0].getClass().getName();
   SClass=SClass.substring(SClass.lastIndexOf('.')+1);
  }
  else SClass="unknown";
  pf.println(this.nlyr+" "+this.nx+","+this.ny+" "+SClass+" "+mbuff);
 }

  void saveLink(PrintStream pf)
 {
  int i,n;
  for (n=0;n<this.node.length;n++)
   {pf.println(n+" "+(this.node[n].lnk.length-1)+" "+this.node[n].lnk[0].wgt);
    for(i=1;i<this.node[n].lnk.length;i++) {
	
	  String w = String.valueOf(this.node[n].lnk[i].wgt);
	  if (w.indexOf("E") != -1) w="0.00001";
	  pf.print(this.node[n].lnk[i].nfrom.nlyr+","+this.node[n].lnk[i].nfrom.nn+":"+w+" ");
	}
    //pf.print(this.node[n].lnk[i].nfrom.nlyr+","+this.node[n].lnk[i].nfrom.nn+":"+this.node[n].lnk[i].wgt+" ");
    if (this.node[n].lnk.length>1) pf.println(" ");
   }
 }


  void loadLink(StreamTokenizer tok, Layer layer[])
 {
   int i,l,nn,nlink,lfrom,nnfrom;
   float wgt;
   for (i=0;i<this.node.length;i++) {
   	try {
    	tok.nextToken(); nn=(int)tok.nval;
    	tok.nextToken(); nlink=(int)tok.nval;
    	this.node[i].lnk= new Link[nlink+1];
    	tok.nextToken();this.node[i].lnk[0]=new Link((float)tok.nval,null);
    	for (l=1;l<this.node[i].lnk.length;l++) {
     		tok.nextToken();lfrom=(int)tok.nval;
     		tok.nextToken();nnfrom=(int)tok.nval;
     		tok.nextToken();wgt=(float)tok.nval;
     		//tok.nextToken();lfrom=Integer.parseInt(tok.sval);
     		//tok.nextToken();nnfrom=Integer.parseInt(tok.sval);
     		//tok.nextToken();wgt=Float.parseFloat(tok.sval);
			//if (lfrom<0 || nnfrom<0) System.out.println(nlyr+" "+i+" "+l+" "+lfrom+" "+nnfrom+" "+tok.nval);
     		this.node[i].lnk[l]= new Link(wgt,layer[lfrom].node[nnfrom]);
    	}
    }
    catch (IOException e){NNError.err("Error on net file");return;}
   }
 }

}
/************************************************************************/
																								
