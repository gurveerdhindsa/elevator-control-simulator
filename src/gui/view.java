package gui;
import main.SchedulerElevators;
import java.awt.GridLayout;
import javax.swing.*;

public class view extends JFrame{
	
	private JFrame frame;
	private JLabel eleLabel1;
	private JLabel eleLabel2;
	private JLabel eleLabel3;
	private JLabel eleLabel4;
	
	public JTextField eleField1;
	public JTextField eleField2;
	public JTextField eleField3;
	public JTextField eleField4;
	
	
	public view (String name) {
		frame = new JFrame(name);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(600, 600);
		frame.setLayout(new GridLayout(5,2));
		addComponents();
		frame.setVisible(true);	
		
	}
	
	
	
	private void addComponents() {
		
		eleLabel1 = new JLabel("Current Floor");
		this.frame.add(eleLabel1);
		eleField1 = new JTextField();
		this.frame.add(eleField1);
		
		/*eleLabel2 = new JLabel("Elevator 2");
		this.frame.add(eleLabel2);
		eleField2 = new JTextField();
		this.frame.add(eleField2);
	
		eleLabel3 = new JLabel ("Elevator 3");
		this.frame.add(eleLabel3);
		eleField3 = new JTextField();
		this.frame.add(eleField3);
		
		eleLabel4 = new JLabel ("Elevator 4");
		this.frame.add(eleLabel4);
		eleField4 = new JTextField();
		this.frame.add(eleField4);
		*/
		
		// make all false
		eleField1.setVisible(true); 
		//eleField2.setVisible(true);
		//eleField3.setVisible(true);
		//eleField4.setVisible(true);
		
		
	}



	public static void main (String[] args) {
		//view guiView =new view();
	}

}
