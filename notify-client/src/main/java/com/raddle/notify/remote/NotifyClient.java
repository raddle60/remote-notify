package com.raddle.notify.remote;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import com.raddle.nio.mina.cmd.SessionCommandSender;
import com.raddle.nio.mina.cmd.api.CommandSender;
import com.raddle.nio.mina.cmd.handler.AbstractCommandHandler;
import com.raddle.nio.mina.hessian.HessianDecoder;
import com.raddle.nio.mina.hessian.HessianEncoder;

public class NotifyClient {
	static {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
		}
	}

	private JFrame jFrame = null; // @jve:decl-index=0:visual-constraint="10,10"
	private JDesktopPane jDesktopPane = null;
	private JScrollPane jScrollPane = null;
	private JTextArea jTextArea = null;
	private JLabel jLabel = null;
	private JTextField rgbTxt = null;
	private JLabel jLabel1 = null;
	private JTextField pointTxt = null;
	private JLabel jLabel2 = null;
	private JTextField serverTxt = null;
	private JButton saveBtn = null;
	private Properties properties = new Properties();
	private File propFile = new File(System.getProperty("user.home")+"/remote-notify.properties");
	NioSocketConnector connector = new NioSocketConnector();

	/**
	 * This method initializes jFrame
	 * 
	 * @return javax.swing.JFrame
	 */
	private JFrame getJFrame() {
		if (jFrame == null) {
			jFrame = new JFrame();
			jFrame.setLocationRelativeTo(null);
			jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			jFrame.setSize(547, 276);
			jFrame.setContentPane(getJDesktopPane());
			jFrame.setTitle("通知客户端");
			jFrame.addWindowListener(new WindowAdapter() {

				@Override
				public void windowClosed(WindowEvent e) {
					connector.dispose();
					super.windowClosed(e);
				}
			});
		}
		return jFrame;
	}

	/**
	 * This method initializes jDesktopPane
	 * 
	 * @return javax.swing.JDesktopPane
	 */
	private JDesktopPane getJDesktopPane() {
		if (jDesktopPane == null) {
			jLabel2 = new JLabel();
			jLabel2.setBounds(new Rectangle(12, 141, 60, 25));
			jLabel2.setText("Server");
			jLabel1 = new JLabel();
			jLabel1.setBounds(new Rectangle(255, 104, 59, 24));
			jLabel1.setText("Point");
			jLabel = new JLabel();
			jLabel.setBounds(new Rectangle(12, 102, 60, 24));
			jLabel.setText("RGB");
			jDesktopPane = new JDesktopPane();
			jDesktopPane.add(getJScrollPane(), null);
			jDesktopPane.add(jLabel, null);
			jDesktopPane.add(getRgbTxt(), null);
			jDesktopPane.add(jLabel1, null);
			jDesktopPane.add(getPointTxt(), null);
			jDesktopPane.add(jLabel2, null);
			jDesktopPane.add(getServerTxt(), null);
			jDesktopPane.add(getSaveBtn(), null);
		}
		return jDesktopPane;
	}

	/**
	 * This method initializes jScrollPane
	 * 
	 * @return javax.swing.JScrollPane
	 */
	private JScrollPane getJScrollPane() {
		if (jScrollPane == null) {
			jScrollPane = new JScrollPane();
			jScrollPane.setBounds(new Rectangle(9, 5, 522, 86));
			jScrollPane.setViewportView(getJTextArea());
		}
		return jScrollPane;
	}

	/**
	 * This method initializes jTextArea
	 * 
	 * @return javax.swing.JTextArea
	 */
	private JTextArea getJTextArea() {
		if (jTextArea == null) {
			jTextArea = new JTextArea();
		}
		return jTextArea;
	}

	/**
	 * This method initializes rgbTxt
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getRgbTxt() {
		if (rgbTxt == null) {
			rgbTxt = new JTextField();
			rgbTxt.setBounds(new Rectangle(81, 102, 154, 25));
		}
		return rgbTxt;
	}

	/**
	 * This method initializes pointTxt
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getPointTxt() {
		if (pointTxt == null) {
			pointTxt = new JTextField();
			pointTxt.setBounds(new Rectangle(329, 104, 197, 26));
		}
		return pointTxt;
	}

	/**
	 * This method initializes serverTxt
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getServerTxt() {
		if (serverTxt == null) {
			serverTxt = new JTextField();
			serverTxt.setBounds(new Rectangle(80, 138, 154, 29));
		}
		return serverTxt;
	}

	private void init() {
		//////////////// load configuration
		if(propFile.exists()){
			try {
				properties.load(new InputStreamReader(new FileInputStream(propFile),"utf-8"));
				getServerTxt().setText(properties.getProperty("server"));
				getRgbTxt().setText(properties.getProperty("rgb"));
				getPointTxt().setText(properties.getProperty("point"));
			} catch (Exception e1) {
				getJTextArea().setText(e1.getMessage());
			}
		}
		////////////////
		try {
			new Timer(true).schedule(new TimerTask() {
				private Robot robot = new Robot();

				@Override
				public void run() {
					try {
						Point point = getComparePoint();
						if (point != null) {
							Color c = robot.getPixelColor((int) point.getX(), (int) point.getY());
							getJTextArea().setText("在(" + ((int) point.getX()) + "," + ((int) point.getY()) + ")捕获的颜色" + c.getRed() + "," + c.getGreen() + "," + c.getBlue());
							Color cc = getCompareColor();
							if (cc != null && !cc.equals(c)) {
								getJTextArea().setText("在(" + ((int) point.getX()) + "," + ((int) point.getY()) + ")颜色发生变化，捕获的颜色(" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + ")，比较的颜色" + cc.getRed() + "," + cc.getGreen() + "," + cc.getBlue());
								sendMessage(true);
							} else {
								sendMessage(false);
							}
						}
					} catch (Exception e) {
						getJTextArea().setText(e.getMessage());
					}
				}
			}, 500, 1000);
		} catch (AWTException e) {
			getJTextArea().setText(e.getMessage());
		}
		/////////// connector setting
		connector.setConnectTimeoutMillis(1000);
		connector.getFilterChain().addFirst("binaryCodec", new ProtocolCodecFilter(new HessianEncoder(), new HessianDecoder()));
		// 处理接收的命令和响应
		connector.setHandler(new AbstractCommandHandler() {
			@Override
			public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
				session.close(true);
			}

			@Override
			protected Object processCommand(Object command) {
				return null;
			}

			@Override
			public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
				session.close(true);
			}
		});
	}

	private Point getComparePoint() {
		if (pointTxt.getText().length() > 0) {
			String[] pp = pointTxt.getText().split(",");
			if (pp.length == 2) {
				try {
					int x = Integer.parseInt(pp[0]);
					int y = Integer.parseInt(pp[1]);
					return new Point(x, y);
				} catch (NumberFormatException e) {
				}
			}
		}
		return null;
	}

	private Color getCompareColor() {
		if (rgbTxt.getText().length() > 0) {
			try {
				if (rgbTxt.getText().indexOf(',') != -1) {
					String[] rgbStr = rgbTxt.getText().split(",");
					if (rgbStr.length == 3) {
						int[] rgb = new int[3];
						rgb[0] = Integer.parseInt(rgbStr[0]);
						rgb[1] = Integer.parseInt(rgbStr[1]);
						rgb[2] = Integer.parseInt(rgbStr[2]);
						return new Color(rgb[0], rgb[1], rgb[2]);
					}
				} else if (rgbTxt.getText().startsWith("0x")) {
					String rgbStr = rgbTxt.getText().substring(2);
					return new Color(Integer.parseInt(rgbStr, 16));
				}
			} catch (NumberFormatException e) {
			}
		}
		return null;
	}

	/**
	 * This method initializes saveBtn	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getSaveBtn() {
		if (saveBtn == null) {
			saveBtn = new JButton();
			saveBtn.setBounds(new Rectangle(253, 141, 104, 26));
			saveBtn.setText("保存配置");
			saveBtn.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					properties.setProperty("server", getServerTxt().getText());
					properties.setProperty("rgb", getRgbTxt().getText());
					properties.setProperty("point", getPointTxt().getText());
					try {
						properties.store(new OutputStreamWriter(new FileOutputStream(propFile),"utf-8"), "");
						getJTextArea().setText("保存配置成功");
					} catch (Exception e1) {
						getJTextArea().setText(e1.getMessage());
					}
				}
			});
		}
		return saveBtn;
	}
	
	private void sendMessage(boolean changed){
		if(getServerTxt().getText().length() > 0){
			if(connector.getManagedSessionCount() == 0){
				try {
					String[] ss = getServerTxt().getText().split(":");
					if(ss.length == 2){
						ConnectFuture future = connector.connect(new InetSocketAddress(ss[0], Integer.parseInt(ss[1])));
						future.awaitUninterruptibly();
						IoSession session = future.getSession();
						CommandSender sender = new SessionCommandSender(session);
						if(changed){
							sender.sendCommand("new");
						} else {
							sender.sendCommand("none");
						}
					}
				} catch (Exception e) {
					getJTextArea().setText(e.getMessage());
				}
			} else {
				try {
					CommandSender sender = new SessionCommandSender(connector.getManagedSessions().values().iterator().next());
					if(changed){
						sender.sendCommand("new");
					} else {
						sender.sendCommand("none");
					}
				} catch (Exception e) {
					getJTextArea().setText(e.getMessage());
				}
			}
		}
	}

	/**
	 * Launches this application
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				NotifyClient application = new NotifyClient();
				application.init();
				application.getJFrame().setVisible(true);
			}
		});
	}

}
