package pez.nn.nrlibj;

class Link
{
 protected float wgt;
 protected float wgtb;
 protected Node nfrom;
 Link(float wgt, Node nfrom){this.wgt=wgt;this.nfrom=nfrom;}
 public int getNfrom(){if (nfrom!=null) return nfrom.nn;else return -1;}
 public int getLfrom(){if (nfrom!=null) return nfrom.nlyr;else return -1;}
}
