package gui;
import main.SchedulerElevators;
import java.awt.GridLayout;
import javax.swing.*;

public class view extends JFrame{
	
	public JFrame frame;
	private JLabel eleLabel1;
	private JLabel dirLabel;

	
	public JTextField eleField1;
	public JTextField dirField;
	
	
	public view (String name) {
		frame = new JFrame(name);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(600, 600);
		frame.setLayout(new GridLayout(0,2));
		addComponents();
		frame.setVisible(true);	
		
	}
	
	
	
	private void addComponents() {
		
		eleLabel1 = new JLabel("Current Floor");
		dirLabel = new JLabel("Direction");
		this.frame.add(eleLabel1);
		this.frame.add(dirLabel);
		eleField1 = new JTextField();
		dirField = new JTextField();
		this.frame.add(eleField1);
		this.frame.add(dirField);

		
		// make all false
		eleField1.setVisible(true); 

		
		
	}



	public static void main (String[] args) {
		
	}

}
