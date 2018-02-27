package com.lapissea.vulkanimpl;

import javax.swing.*;

import static javax.swing.WindowConstants.*;

public class FailMsg{
	private JPanel  mainPanel;
	private JButton exitButton;
	private JLabel  errorLabel;
	
	public FailMsg(){ exitButton.addActionListener(e->System.exit(0));}
	
	public static void create(String msg){
		try{ UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }catch(Exception e){}
		
		JFrame frame=new JFrame();
		frame.setTitle("Startup error");
		FailMsg failMsg=new FailMsg();
		failMsg.errorLabel.setText(msg);
		frame.getContentPane().add(failMsg.mainPanel);
		frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
		frame.pack();
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}
}
