/**
 * Created by dragoon on 11/27/16.
 */
public class YYY_Pair<K, V> {
	private K first;
	private V second;

	public YYY_Pair(K first, V second) {
		this.first = first;
		this.second = second;
	}

	public YYY_Pair(YYY_Pair<K, V> pair) {
		this.first = pair.first;
		this.second = pair.second;
	}


	public K getFirst() {
		return first;
	}

	public void setFirst(K first) {
		this.first = first;
	}

	public V getSecond() {
		return second;
	}

	public void setSecond(V second) {
		this.second = second;
	}
}
