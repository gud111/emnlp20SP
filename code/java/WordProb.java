package cc.mallet.topics;

public class WordProb implements Comparable {
	int wi;
	double p;
	public WordProb (int wi, double p) { this.wi = wi; this.p = p; }
	public final int compareTo (Object o2) {
		if (p > ((WordProb)o2).p)
			return -1;
		else if (p == ((WordProb)o2).p)
			return 0;
		else return 1;
	}
}