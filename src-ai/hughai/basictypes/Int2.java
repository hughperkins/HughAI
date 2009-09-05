package hughai.basictypes;

// do we want immutable or not?
public class Int2 {
	int x;
	int y;
	
	public Int2(){}
	public Int2( int x, int y ){ this.x = x; this.y = y; }
	public Int2( Int2 source ) {
		this.x = source.getX();
		this.y = source.getY();
	}
	public int getX() {
		return x;
	}
	public int getY() {
		return y;
	}
}
