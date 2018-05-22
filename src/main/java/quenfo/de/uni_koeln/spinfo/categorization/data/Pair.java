package quenfo.de.uni_koeln.spinfo.categorization.data;

public class Pair implements Comparable<Pair> {

	private Entity e1;
	private Entity e2;
	private double score;

	public Pair(Entity e1, Entity e2) {
		String s1 = e1.getLemma();
		String s2 = e2.getLemma();
		if (s1.compareTo(s2) > 0) {
			this.e1 = e1;
			this.e2 = e2;
		} else {
			this.e1 = e2;
			this.e2 = e1;
		}
	}

	public Pair(String s1, String s2) {
		Entity e1 = new Entity(s1);
		Entity e2 = new Entity(s2);
		if (s1.compareTo(s2) > 0) {
			this.e1 = e1;
			this.e2 = e2;
		} else {
			this.e1 = e2;
			this.e2 = e1;
		}
	}

	public Entity getE1() {
		return e1;
	}

	public void setE1(Entity e1) {
		this.e1 = e1;
	}

	public Entity getE2() {
		return e2;
	}

	public void setE2(Entity e2) {
		this.e2 = e2;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	@Override
	public boolean equals(Object o) {
		Pair pair = (Pair) o;
		if (this.e1.equals(pair.getE1())) {
			return this.e2.equals(pair.getE2());
		}
		if (this.e1.equals(pair.getE2())) {
			return this.e2.equals(pair.getE1());
		}
		return false;
	}

	@Override
	public int hashCode() {
		String s = e1.getLemma() + e2.getLemma();
		return s.hashCode();
	}

	@Override
	public String toString() {
		return this.e1 + " - " + this.e2;
	}

	@Override
	public int compareTo(Pair pair) {
		if (this.equals(pair))
			return 0;
		String s1 = this.getE1().getLemma() + this.getE2().getLemma();
		String s2 = pair.getE1().getLemma() + pair.getE2().getLemma();
		return s1.compareTo(s2);
	}

}
