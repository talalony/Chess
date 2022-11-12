import java.awt.*;
import javax.swing.JFrame;

public class GameFrame extends JFrame {
	Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
	boolean fullScreen = false;
	public GameFrame() {
		this.setTitle("Chess");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setResizable(false);
		int f = greatestCommonFactor(dim.width, dim.height);
		if (dim.width/f == 16 && dim.height/f == 9){
			this.setExtendedState(JFrame.MAXIMIZED_BOTH);
			this.setUndecorated(true);
			this.setAlwaysOnTop(true);
			fullScreen = true;
		}
		this.add(new GamePanel(this));
		this.pack();
		this.setVisible(true);
		this.setLocationRelativeTo(null);
	}

	public static int greatestCommonFactor(int width, int height) {
		return (height == 0) ? width : greatestCommonFactor(height, width % height);
	}

}