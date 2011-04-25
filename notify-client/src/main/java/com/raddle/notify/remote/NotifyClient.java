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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
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

import com.raddle.nio.codec.impl.HessianCodec;
import com.raddle.nio.mina.cmd.SessionCommandSender;
import com.raddle.nio.mina.cmd.api.CommandSender;
import com.raddle.nio.mina.cmd.handler.AbstractCommandHandler;
import com.raddle.nio.mina.codec.ChainCodecFactory;
import com.raddle.notify.remote.bean.PositionColor;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

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
	private JLabel jLabel2 = null;
	private JTextField serverTxt = null;
	private JButton saveBtn = null;
	private Properties properties = new Properties();
	private File propFile = new File(System.getProperty("user.home")+"/remote-notify.properties");
	private NioSocketConnector connector = new NioSocketConnector();  //  @jve:decl-index=0:
	private List<PositionColor> listPsColor = new ArrayList<PositionColor>();  //  @jve:decl-index=0:
	private JLabel jLabel = null;

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
			jFrame.setSize(545, 363);
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
			jLabel = new JLabel();
			jLabel.setBounds(new Rectangle(10, 214, 521, 33));
			jLabel.setText("");
			jLabel2 = new JLabel();
			jLabel2.setBounds(new Rectangle(13, 254, 60, 25));
			jLabel2.setText("Server");
			jDesktopPane = new JDesktopPane();
			jDesktopPane.add(getJScrollPane(), null);
			jDesktopPane.add(jLabel2, null);
			jDesktopPane.add(getServerTxt(), null);
			jDesktopPane.add(getSaveBtn(), null);
			jDesktopPane.add(jLabel, null);
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
			jScrollPane.setBounds(new Rectangle(9, 5, 522, 196));
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
	 * This method initializes serverTxt
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getServerTxt() {
		if (serverTxt == null) {
			serverTxt = new JTextField();
			serverTxt.setBounds(new Rectangle(83, 253, 154, 29));
		}
		return serverTxt;
	}

	@SuppressWarnings("unchecked")
	private void init() {
		//////////////// load configuration
		InputStream colorInputStream = this.getClass().getResourceAsStream("/postion-color.xml");
		if(colorInputStream != null){
	    	XStream xstream = new XStream(new DomDriver());
	    	xstream.alias("postion-color", ArrayList.class);
	    	xstream.alias("item", PositionColor.class);
			listPsColor =  (List<PositionColor>) xstream.fromXML(colorInputStream);
		}
		if(propFile.exists()){
			try {
				properties.load(new InputStreamReader(new FileInputStream(propFile),"utf-8"));
				getServerTxt().setText(properties.getProperty("server"));
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
					StringBuilder sb = new StringBuilder();
					boolean changed = false;
					try {
						if(listPsColor == null || listPsColor.size() == 0){
							sb.append("没有配置比较的颜色").append("\n");
							changed = true;
						}
						for (PositionColor positionColor : listPsColor) {
							Point point = getComparePoint(positionColor.getPostion());
							if (point != null) {
								Color c = robot.getPixelColor((int) point.getX(), (int) point.getY());
								Color cc = getCompareColor(positionColor.getColor());
								if (cc != null) {
									if(positionColor.isEqual() && cc.equals(c)){
										sb.append("在(" + ((int) point.getX()) + "," + ((int) point.getY()) + ")颜色等于捕获的颜色(" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + ")，比较的颜色" + cc.getRed() + "," + cc.getGreen() + "," + cc.getBlue()).append("\n");
										changed = true;
									} else if(!positionColor.isEqual() && !cc.equals(c)){
										sb.append("在(" + ((int) point.getX()) + "," + ((int) point.getY()) + ")颜色发生变化，捕获的颜色(" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + ")，比较的颜色" + cc.getRed() + "," + cc.getGreen() + "," + cc.getBlue()).append("\n");
										changed = true;
									} else {
										sb.append("在(" + ((int) point.getX()) + "," + ((int) point.getY()) + ")捕获的颜色" + c.getRed() + "," + c.getGreen() + "," + c.getBlue()+", 没有变化").append("\n");
									}
								} else {
									sb.append("在(" + ((int) point.getX()) + "," + ((int) point.getY()) + ")捕获的颜色" + c.getRed() + "," + c.getGreen() + "," + c.getBlue()+", 没有配置比较颜色").append("\n");
								}
							} else {
								sb.append("没有配置取点坐标").append("\n");
							}
						}
					} catch (Exception e) {
						sb.append("\n").append(e.getMessage());
					}
					sendMessage(changed);
					getJTextArea().setText(sb.toString());
				}
			}, 500, 1000);
		} catch (AWTException e) {
			getJTextArea().setText(e.getMessage());
		}
		/////////// connector setting
		connector.setConnectTimeoutMillis(1000);
		ChainCodecFactory chainCodecFactory = new ChainCodecFactory();
		chainCodecFactory.addFirst(new HessianCodec());
		connector.getFilterChain().addFirst("binaryCodec", new ProtocolCodecFilter(chainCodecFactory));
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

			@Override
			protected String getExecuteQueue(Object command) {
				return null;
			}
		});
	}

	private Point getComparePoint(String point) {
		if (point != null && point.length() > 0) {
			String[] pp = point.split(",");
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

	private Color getCompareColor(String color) {
		if (color != null && color.length() > 0) {
			try {
				if (color.indexOf(',') != -1) {
					String[] rgbStr = color.split(",");
					if (rgbStr.length == 3) {
						int[] rgb = new int[3];
						rgb[0] = Integer.parseInt(rgbStr[0]);
						rgb[1] = Integer.parseInt(rgbStr[1]);
						rgb[2] = Integer.parseInt(rgbStr[2]);
						return new Color(rgb[0], rgb[1], rgb[2]);
					}
				} else if (color.startsWith("0x")) {
					String rgbStr = color.substring(2);
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
			saveBtn.setBounds(new Rectangle(250, 255, 104, 26));
			saveBtn.setText("保存配置");
			saveBtn.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					properties.setProperty("server", getServerTxt().getText());
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
					jLabel.setText("");
				} catch (Exception e) {
					jLabel.setText("发送异常：" + e.getMessage());
				}
			} else {
				try {
					CommandSender sender = new SessionCommandSender(connector.getManagedSessions().values().iterator().next());
					if(changed){
						sender.sendCommand("new");
					} else {
						sender.sendCommand("none");
					}
					jLabel.setText("");
				} catch (Exception e) {
					jLabel.setText("发送异常：" + e.getMessage());
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
