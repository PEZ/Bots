package pez.nn.nrlibj;
import java.io.*;
import java.lang.*;
import java.lang.reflect.*;
import java.util.*;

/************************************************************************/
/*                                                                      */
/*                    CLASS  NrPop                                      */
/*                                                                      */
/*                    (vector of NNet)                                  */
/*                                                                      */
/************************************************************************/


/**
* This class defines and holds a population of neural network objects.
* It olds neural networs in a vector but the first network has number 1.
* (it doesn't exist the network number 0) <BR>
* You can create a population of simple 3 layers networks (with or without Elman
* memory context) using simple constructor. Or you can create a population of
* very complicated neural networks using a description language.<BR>
* Each type of constructor call corrispondent constructor for single neural
* and assigns this neural network objects to vector elements.<BR>
* In addition this class contain as static member some default values and some
* utility methods as random generator and file-to-stringarray utility
* @author D. Denaro
* @version 5.0 , 2/2001
*/
public class NrPop
{
 static boolean fbias=false;
 static float eps=(float)0.3;
 static float alfa=(float)0.3;
 static float ra=(float)-0.6;
 static float rb=(float)+0.6;
 public static Rand rnd= new Rand();


 /**
 * It sets the default range for random weigth choosing (def. -0.6 +0.6)
 */
 public static  void setDefRandRange(float ra,float rb){NrPop.ra=ra;NrPop.rb=rb;}
 /**
 * It sets the default learning factor in EBP procedure (def. 0.3)
 */
 public static  void setDefLearnFactor(float e){NrPop.eps=e;}
 /**
 * It sets the default momentum factor in EBP procedure (def. 0.3)
 */
 public static  void setDefMomentFactor(float a){NrPop.alfa=a;}
 /**
 * It sets the default flag for bias training in EBP procedure (def. false)
 */
 public static  void setDefFlagBiasLearn(boolean fbias){NrPop.fbias=fbias;}
 /**
 * It reads the default learning factor in EBP procedure
 */
 public static  float readDefLearnFactor(){return NrPop.eps;}
 /**
 * It reads the default momentum factor in EBP procedure
 */
 public static  float readDefMomentFactor(){return NrPop.alfa;}
 /**
 * It reads the default flag for bias training in EBP procedure
 */
 public static  boolean readDefFlagBiasLearn(){return NrPop.fbias;}

 /**
 * This method set seed for random generator (same sequence with same seed)
 */
 public static void setSeed(int s){rnd.setSeed((long)s);}
 /**
 * This method set seed for random generator using the time (it is the default)
 */
 public static void setSeed(){rnd.setSeed(System.currentTimeMillis());}
 /**
 * This method return a random integer number between a and b (included)
 */
 public static int riab(int a,int b) {return rnd.iab(a,b);}
 /**
 * This method return a random float number between a and b (included)
 */
 public static float rfab(float a, float b) {return rnd.fab(a,b);}
 /**
 * This method return a random float number with gaussian distribution with <TT>"dev"</TT>
 * as standard deviation
 */
 public static float rgauss(float dev) {return rnd.gauss(dev);}

 Vector Pnet;
 Fitness fitness;

 /**
 * This constructor creates an empty population of <TT>"peoplenum"</TT> dimension
 */
 public NrPop(int peoplenum) {Pnet= new Vector(peoplenum);}

 /**
 * This constructor creates a population of <TT>"peoplenum"</TT> networks, heach of them
 * with tree layers and a buffer memory (0,1,2,3) or not (0,1,2). <BR>
 * When <TT>"mem"</TT> = true, a buffer of hiden layer is created and this
 * memory layer is linked with the hiden layer (Elman context memory)<BR>
 * The weights of the net are randomicaly choosen with range <TT>"ra"</TT> - <TT>"rb"</TT>.<BR>
 * <TT>"nodeinp"</TT> is the number of first layer nodes, <TT>"nodehid"</TT> of hiden layer and
 * <TT>"nodeout"</TT> of last layer. <BR>
 * The first layer nodes (layer 0) are liner node (NodeLin); but in hidden layer
 * and in last layer nodes ave sigmoid transfer function of activation (NodeSigm).
 */
 public NrPop(int peoplenum,int nodeinp,int nodehid,int nodeout,boolean mem,float ra,float rb)
 {int i; Pnet= new Vector(peoplenum);
  for (i=1;i<=peoplenum;i++) Pnet.add(new NNet(nodeinp,nodehid,nodeout,mem,ra,rb));
 }

 /**
 * This constructor creates a population of <TT>"peoplenum"</TT> networks, heach of them
 * with tree layers and a buffer memory or not. <BR>
 * When <TT>"mem"</TT> = true, a buffer of hiden layer is created and this
 * memory layer is linked with the hiden layer (Elman context memory) <BR>
 * The weights of the net are randomicaly choosen with default range.<BR>
 * <TT>"nodeinp"</TT> is the number of first layer nodes, <TT>"nodehid"</TT> of hiden layer and
 * <TT>"nodeout"</TT> of last layer. <BR>
 * The first layer nodes (layer 0) are liner node (NodeLin); but in hidden layer
 * and in last layer nodes ave sigmoid transfer function of activation (NodeSigm)
 */
 public NrPop(int peoplenum,int nodeinp,int nodehid,int nodeout,boolean mem)
 {this(peoplenum,nodeinp,nodehid,nodeout,mem,NrPop.ra,NrPop.rb);}

 /**
 * This constructor creates a population of networks in the most flexible way.
 * The string array <TT>"descrrec"</TT> contains the records that describes the network
 * population in a simple predefined laguage:
 * <PRE>
 * 4 records types
 * <B>
 *  net=n[,m] </B>
 *   meens: next definition is for net number "n" or for all nets from "n" to "m"
 * <B>
 *  layer=n [tnode=n,[m]  nname=xxxx... copytoml=n]  </B>
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
 *  biasval=na[,nb]  oflayer=n[(h[,k])]  </B>
 *   meens: as previous kind of record but for bias values
 *
 * example:
 *
 * net=5,10
 * layer=0 tnode=2  nname=NodeLin
 * layer=1 tnode=2  nname=NodeSigm
 * layer=2 tnode=1  nname=NodeSigm
 * linktype=all fromlayer=0  tolayer=1
 * linktype=all fromlayer=1  tolayer=2
 *
 * </PRE>
 * it defines 6 networks with 3 layers and 2,2,1 nodes (for XOR problem for instance)
 * NodeLin is a kind of node that reproduce on output the input value
 * NodeSigm is a sigmoide node.
 */
 public NrPop(String descrrec[])
 {
  int i,n,fromnet=1,tonet=1;
  String t,val;
  String a,b,c;
  StringTokenizer tok;
  Pnet= new Vector();
  for (i=0;i<descrrec.length;i++)
  {
    tok=new StringTokenizer(descrrec[i]);
    if (tok.countTokens()<1) continue;
    t=tok.nextToken();
    if (t.startsWith("net="))
       {val=t.substring(t.indexOf('='));a=values(val,1);b=values(val,2);
        try {fromnet=Integer.parseInt(a);} catch (NumberFormatException e){NNError.err("Net Number missed at rec "+i);}
        try {tonet=Integer.parseInt(b);} catch (NumberFormatException e){tonet=fromnet;}
        if (tonet>this.PopSize()) this.setSize(tonet);
        for (n=fromnet;n<=tonet;n++){NNet nnet= new NNet(descrrec,i+1);this.setNNet(n,nnet);}
       }
  }
 }

 /**
 * This constructor opens a file named <TT>"descrfile"</TT> and read networks description.
 * Similar to constructor NrPop(String Descrrec[]) but useful for external networks definition.
 */
 public NrPop(String descrfile)
 {
  int i,n,fromnet=1,tonet=1;
  boolean endfile=false;
  String t="",val,line;
  String a,b,c;
  Vector rec=new Vector(100);
  String descrnet[];
  StringTokenizer tok;
  FileReader dfile;
  LineNumberReader dline;
  try
  {
    dfile= new FileReader(descrfile);
    dline= new LineNumberReader(dfile);
    try{ do
    {tok=new StringTokenizer(dline.readLine()); t=tok.nextToken();}
     while (t.startsWith("net="));}
    catch (IOException e){endfile=true;}
    while (!endfile)
    {
     val=t.substring(t.indexOf('='));a=values(val,1);b=values(val,2);
     try {fromnet=Integer.parseInt(a);} catch (NumberFormatException e){NNError.err("Net Number missed at rec "+dline.getLineNumber());}
     try {tonet=Integer.parseInt(b);} catch (NumberFormatException e){tonet=fromnet;}
     if (tonet>this.PopSize()) this.setSize(tonet);
     while(true)
     { try {line=dline.readLine();tok=new StringTokenizer(line);t=tok.nextToken();
            if (t.startsWith("net=")) break; else rec.addElement(line);}
       catch (IOException e) {endfile=true;break;}
     }
     if (endfile)break;
     descrnet=(String[])rec.toArray();
     for (n=fromnet;n<=tonet;n++){NNet nnet= new NNet(descrnet,0);this.setNNet(n,nnet);}
    }
    try{dline.close();dfile.close();} catch (IOException e){};
  }
  catch (FileNotFoundException e){NNError.err("File not found "+descrfile);}
 }

 /**
 * Returns a pointer to a net that is in the <TT>"index"</TT> position of population vector.
 * In a population NrPop the nets are numbered starting from 1
 */
 public NNet getNNet(int index){return (NNet)Pnet.get(index-1);}
 /**
 * Increases vector size of 1 and put the net pointer in this last position.
 * In a population NrPop the nets are numbered starting from 1
 */
 public void addNNet(NNet nnet){Pnet.add(nnet);}
 /**
 * Puts the net pointer in this <TT>"index"</TT> position.
 * In a population NrPop the nets are numbered starting from 1
 */
 public void setNNet(int index,NNet nnet){Pnet.setElementAt(nnet,index-1);}
 /**
 * Returns the population size (vector dimension).
 * In a population NrPop the nets are numbered starting from 1
 */
 public int PopSize(){return Pnet.size();}
 /**
 * Sets the size o population vector (increase o decrease with null values).
 * In a population NrPop the nets are numbered starting from 1
 */
 public void setSize(int s){Pnet.setSize(s);}
 /**
 * Removes the net at <TT>"index"</TT> position, and vector size is reduced (all next nets
 * are shifted back).
 * In a population NrPop the nets are numbered starting from 1
 */
 public void removeNNet(int index){Pnet.removeElementAt(index-1);}

 /**
 * Initializes fitness array (it must be called before using other fitness methods).
 * <BR> Or if PopSize is changed.
 */
 public void fitInit()
 {
  int i;
  fitness= new Fitness(Pnet.size());
  for (i=1;i<=Pnet.size();i++){fitness.setFit(i,0);}
 }

 /**
 * Set fitness value for NNet number <TT>"netnumber"</TT> .
 */
 public void fitSet(int netnumber,float val){fitness.setFit(netnumber,val);}

 /**
 * Return the NNet number corresponding to <TT>"position"</TT> number in graded list.
 * <BR>Must be utilized after RankingMax or Min call.
 * <BR>First position is 1. Last position is PopSize.
 */
 public int fitGetNumAtPos(int position){return fitness.getNum(position);}

 /**
 * Return the NNet pointer corresponding to <TT>"position"</TT> number in graded list.
 * <BR>Must be utilized after RankingMax or Min call.
 * <BR>First position is 1. Last position is PopSize.
 */
 public NNet fitGetNetAtPos(int position){return getNNet(fitness.getNum(position));}

 /**
 * Return the fitness value corresponding to <TT>"position"</TT> number in graded list.
 * <BR>Must be utilized after RankingMax or Min call.
 * <BR>First position is 1. Last position is PopSize.
 */
 public float fitGetValAtPos(int position){return fitness.getVal(position);}

 /**
 * Order the fitness array in a graded list where the first position corresponds
 * to NNet with minimum fitness value.
 * <BR>Must be utilized after RankingMax or Min call.
 */
 public void fitRankingMin(){fitness.rankMin();}

 /**
 * Order the fitness array in a graded list where the first position corresponds
 * to NNet with maximum fitness value.
 * <BR>Must be utilized after RankingMax or Min call.
 */
 public void fitRankingMax(){fitness.rankMax();}

 /**
 * Return the array corresponding to the graded list of NNet number.
 * <BR>Must be utilized after RankingMax or Min call.
 * <BR>In this case first position is 0 and last position is PopSize-1.
 */
 public int[] fitGetGradingList(){return fitness.getArrayNum();}

 /**
 * Return the fitness stored value of NNet number <TT>"netnumber"</TT>.
 * <BR>It scan the fitness array (expensive)
 */
 public float fitGetFitness(int netnumber)
 {
  int i;
  for (i=0;i<Pnet.size();i++) {if (fitness.net[i]==netnumber) return fitness.fit[i];}
  return 0;
 }

 /**
 * Saves the whole population networks on the file named <TT>"namefile"</TT>.
 * This population can be read with nrPopLoad method
 */
 public void nrPopSave(String nomefile)
 {
   int i;
   FileOutputStream fileout;
   PrintStream pf;
   try
   {
    fileout= new FileOutputStream(nomefile);
    pf=new PrintStream(fileout);
    pf.println("NPOP-DATA "+this.PopSize());
    for (i=1;i<=this.PopSize();i++) this.getNNet(i).saveNNet(pf,i);
    pf.close();
   }
   catch (FileNotFoundException e){NNError.err("Impossible to create file "+nomefile);}
 }


 /**
 * Saves the networks enumerated on <TT>"numnet[]"</TT> array for <TT>"len"</TT> elements.
 * This population can be read with nrPopLoad constructor method
 */
 public void nrPopSave(String nomefile,int numnet[],int len)
 {
   int i,l;
   FileOutputStream fileout;
   PrintStream pf;
   if (numnet.length<len) l=numnet.length;else l=len;
   try
   {
    fileout= new FileOutputStream(nomefile);
    pf=new PrintStream(fileout);
    pf.println("NPOP-DATA "+this.PopSize());
    for (i=0;i<l;i++)
    { if (numnet[i]<= this.PopSize()) this.getNNet(numnet[i]).saveNNet(pf,numnet[i]); }
    pf.close();
   }
   catch (FileNotFoundException e){NNError.err("Impossible to create file "+nomefile);}
 }

 /**
 * This constructor loads a population network saved with nrPopSave method.
 * To distinguish this constructor from a new population constructor (that use
 * a description file), this constructor needs a flag <TT>"load"</TT>= true
 */
 public NrPop(String nomefile,boolean load)
 {
   int i;
   int npop=0,net;
   NNet nnet;
   FileInputStream fileinp;
   InputStreamReader fileread;
   StreamTokenizer tok;
   try
   {
    fileinp= new FileInputStream(nomefile);
    fileread= new InputStreamReader(fileinp);
    tok=new StreamTokenizer(fileread);
    tok.whitespaceChars(0x2C,0x2C); /* "," */
    tok.whitespaceChars(0x3A,0x3A); /* ":" */
    try{tok.nextToken();}catch (IOException e){return;}
    if (tok.sval.equals("NPOP-DATA"))
    try {tok.nextToken(); npop=(int)tok.nval;}catch (IOException e){NNError.err("Error on net file");return;}
    this.Pnet=new Vector(npop);
    for (i=1;i<=npop;i++)
     {
      try{tok.nextToken();}catch (IOException e){break;}
      if (tok.sval.equals("NET"))
      try{tok.nextToken(); net=(int)tok.nval;}catch (IOException e){NNError.err("Error on net file");break;}
      nnet= new NNet(); nnet.loadNNet(tok);
      this.addNNet(nnet);
     }
//    fileread.close();
   }
   catch (FileNotFoundException e){NNError.err("File not found "+nomefile);}
 }

 static String values(String val,int n)
 {
  int i,p=1,cp=0;
  if (val.length()==0) return "";
  for (i=1;i<val.length();i++)
  {if ( val.charAt(i)==','|val.charAt(i)=='('|val.charAt(i)==')' )
   {cp++; if (cp==n) return val.substring(p,i);p=i+1;}
  }
  {cp++; if (cp==n) return val.substring(p,i);p=i+1;}
  return "";
 }

 /**
 * This utility reads a file and put each record in a string array and return
 * the pointer. It is useful to optimize the learning or test procedure.
 */
 public static String[] fileToStrArray(String filename)
 {
   Vector rec=new Vector(100);
   String strarray[]=null;
   Object arr[];
   String line;
   FileReader fread;
   LineNumberReader fline;
   try
   {
    fread= new FileReader(filename);
    fline= new LineNumberReader(fread);
    while(true)
    { try {line=fline.readLine();
           if (line==null) break;
           rec.addElement(line) ;}
     catch (IOException e) {break;} }
    strarray=new String[rec.size()];
    arr=rec.toArray(strarray);
    try{fline.close();fread.close();} catch (IOException e){};
    return strarray;
   }catch (FileNotFoundException e) {NNError.err("File not found "+filename);}
   return null;
 }


}

/************************************************************************/

/**
* Class Rand initialize a random generator.
* If called without parameter it use time as seed.
*/

class Rand extends Random
{
 public Rand(int s){super ((long) s);}
 public Rand(){super();}
 public int iab(int a,int b) {return a+nextInt(b-a+1);}
 public float fab(float a, float b) {return a+nextFloat()*(b-a);}
 public float gauss(float dev) {return (float) (nextGaussian()*dev);}
}

/************************************************************************/

class Fitness
{
 int dim;
 float fit[];
 int   net[];
 Fitness(int n){fit=new float[n];net=new int[n];dim=n;}
 void setFit(int nnet,float nfit){net[nnet-1]=nnet;fit[nnet-1]=nfit;}
 void rankMax()
 {int c,i,appo2;
    float appo1;
    for (c=0; c<dim; c++)
    {for (i=dim-1;i>c;i--)
      {if (fit[i] > fit[i-1])
       { appo1=fit[i]; fit[i]=fit[i-1]; fit[i-1]=appo1;
         appo2=net[i]; net[i]=net[i-1]; net[i-1]=appo2;}
      }
    }
 }
 void rankMin()
 {int c,i,appo2;
    float appo1;
    for (c=0; c<dim; c++)
    {for (i=dim-1;i>c;i--)
      {if (fit[i] < fit[i-1])
       { appo1=fit[i]; fit[i]=fit[i-1]; fit[i-1]=appo1;
         appo2=net[i]; net[i]=net[i-1]; net[i-1]=appo2;}
      }
    }
 }
 int getNum(int i){return net[i-1];}
 float getVal(int i){return fit[i-1];}
 int[] getArrayNum(){return net;}
 float[] getArrayVal(){return fit;}
}

