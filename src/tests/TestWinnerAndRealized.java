package tests;
import speed.WinnerAndRealized;

public class TestWinnerAndRealized {

	public static void main(String args[]) {
		int n = 5;
		WinnerAndRealized[] war = new WinnerAndRealized[2];
		war[0] = new WinnerAndRealized(n);
		boolean[] w = new boolean[n];
		int[] d = new int[n];
		
		for (int i = 0; i < n;  i++) {
			w[i] = true;
			d[i] = i;
		}
		
		war[0] = new WinnerAndRealized(w,d);
		System.out.print("war[0].d = [");
		for (int i = 0; i < n; i++)
			System.out.print(war[0].d[i] + " ");
		System.out.println("]");
		System.out.println("war[0].w.length = " + war[0].w.length + ", war[0].d.length = " + war[0].d.length);
		
	}
}
                                                                                                                                                                                             