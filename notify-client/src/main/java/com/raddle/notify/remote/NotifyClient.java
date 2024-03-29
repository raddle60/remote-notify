package com.raddle.notify.remote;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
	private File propFile = new File(System.getProperty("user.home")+"/remote-notify/remote-notify.properties");
	private NioSocketConnector connector = new NioSocketConnector();  //  @jve:decl-index=0:
	private List<PositionColor> listPsColor = new ArrayList<PositionColor>();  //  @jve:decl-index=0:
	private JLabel jLabel = null;
	private BufferedImage noMsgImage = null;  //  @jve:decl-index=0:
	private TrayIcon trayIcon = null;  //  @jve:decl-index=0:
	private JTextArea msgTxt;
	private Robot robot = null;
	private JPanel pickImgPane;

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
			jFrame.setSize(560, 472);
			jFrame.setContentPane(getJDesktopPane());
			jFrame.setTitle("通知客户端");
			jFrame.addWindowListener(new WindowAdapter() {

				@Override
				public void windowClosed(WindowEvent e) {
					connector.dispose();
					super.windowClosed(e);
				}

				@Override
				public void windowIconified(WindowEvent e) {
					jFrame.setVisible(false);
					super.windowIconified(e);
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
			jDesktopPane.setBackground(Color.WHITE);
			jDesktopPane.add(getJScrollPane(), null);
			jDesktopPane.add(jLabel2, null);
			jDesktopPane.add(getServerTxt(), null);
			jDesktopPane.add(getSaveBtn(), null);
			jDesktopPane.add(jLabel, null);
			
			JButton pickColorBtn = new JButton("\u83B7\u53D6\u989C\u8272");
			pickColorBtn.setToolTipText("鼠标按住不放拖拽，松开时取色。拖拽时方向键微调。");
			pickColorBtn.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					if (e.getKeyCode() == KeyEvent.VK_UP) {
						Point mousepoint = MouseInfo.getPointerInfo().getLocation();
						robot.mouseMove(mousepoint.x, mousepoint.y - 1);
					} else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
						Point mousepoint = MouseInfo.getPointerInfo().getLocation();
						robot.mouseMove(mousepoint.x, mousepoint.y + 1);
					} else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
						Point mousepoint = MouseInfo.getPointerInfo().getLocation();
						robot.mouseMove(mousepoint.x - 1, mousepoint.y);
					} else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
						Point mousepoint = MouseInfo.getPointerInfo().getLocation();
						robot.mouseMove(mousepoint.x + 1, mousepoint.y);
					}
				}
			});
			pickColorBtn.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseReleased(MouseEvent e) {
					
				}
			});
			pickColorBtn.addMouseMotionListener(new MouseMotionAdapter() {
				@Override
				public void mouseDragged(MouseEvent e) {
					Point onScreen = e.getLocationOnScreen();
                    int pixSize = 10;
                    int width = pickImgPane.getWidth() - pickImgPane.getWidth() % pixSize;
                    int count = width / pixSize;
					int x = onScreen.x - count / 2;
					int y = onScreen.y - count / 2;
					BufferedImage capture = robot.createScreenCapture(new Rectangle(x, y, width, width));
					Graphics graphics = pickImgPane.getGraphics();
					for (int i = 0; i < count; i++) {
						for (int j = 0; j < count; j++) {
							Color capColor = new Color(capture.getRGB(i, j));
							graphics.setColor(capColor);
							graphics.fillRect(i * pixSize, j * pixSize, pixSize, pixSize);
						}
					}
                    Color c = robot.getPixelColor(onScreen.x, onScreen.y);
                    graphics.setColor(Color.BLACK);
                    graphics.drawLine(width / 2 + pixSize / 2, 0, width / 2 + pixSize / 2, width);
                    graphics.drawLine(0, width / 2 + pixSize / 2, width, width / 2 + pixSize / 2);
                    graphics.setColor(c);
                    graphics.fillRect(width / 2, width / 2, pixSize, pixSize);
                    graphics.setColor(Color.BLACK);
                    graphics.setXORMode(Color.WHITE);
                    graphics.drawRect(width / 2, width / 2, pixSize - 1, pixSize - 1);
                    msgTxt.setText("POSITION:" + onScreen.x + "," + onScreen.y + "\nRGB:" + c.getRed() + "," + c.getGreen() + "," + c.getBlue()
                            + "\nHEX:0x" + Integer.toHexString(c.getRGB()));
				}
			});
			pickColorBtn.setBounds(274, 350, 80, 27);
			jDesktopPane.add(pickColorBtn);
			
			msgTxt = new JTextArea();
			msgTxt.setBounds(10, 299, 250, 115);
			jDesktopPane.add(msgTxt);
			msgTxt.setEditable(false);
			
			pickImgPane = new JPanel();
			pickImgPane.setBounds(361, 254, 160, 160);
			pickImgPane.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			jDesktopPane.add(pickImgPane);
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
		InputStream colorInputStream = this.getClass().getResourceAsStream("/position-color.xml");
		if(colorInputStream == null){
			File file = new File(System.getProperty("user.home")+"/remote-notify/position-color.xml");
			if(file.exists()){
				try {
					colorInputStream = new FileInputStream(file);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
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
			noMsgImage = ImageIO.read(NotifyClient.class.getResourceAsStream("/man.png"));
			SystemTray systemTray = SystemTray.getSystemTray();
			trayIcon = new TrayIcon(noMsgImage, "颜色变化通知监视器");
			systemTray.add(trayIcon);
			trayIcon.addMouseListener(new MouseAdapter() {
				@Override
                public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2) {// 双击托盘窗口再现
						if(jFrame.isVisible()){
							jFrame.setVisible(false);
						} else {
							jFrame.setVisible(true);
							jFrame.setState(Frame.NORMAL);
						}
					}
				}
			});
			robot = new Robot();
		} catch (Exception e) {
			getJTextArea().setText(e.getMessage());
		}
		//////////
		new Timer(true).schedule(new TimerTask() {
			
			@Override
			public void run() {
				StringBuilder sb = new StringBuilder();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				sb.append(sdf.format(new Date()) + " ");
				boolean changed = false;
				try {
					if(listPsColor == null || listPsColor.size() == 0){
						sb.append("没有配置比较的颜色").append("\n");
						changed = true;
					} else {
						for (PositionColor positionColor : listPsColor) {
                            if (positionColor.getMaxNotMatchedTimes() <= 0) {
                                positionColor.setMaxNotMatchedTimes(3);
                            }
                            Point orgPoint = getComparePoint(positionColor.getPostion());
                            if (positionColor.getPostionPoint() == null) {
                                positionColor.setPostionPoint(orgPoint);
                            }
                            Point point = positionColor.getPostionPoint();
							if (point != null) {
								Color c = robot.getPixelColor((int) point.getX(), (int) point.getY());
								positionColor.setPreColor(positionColor.getCurColor());
								positionColor.setCurColor(c);
								if(positionColor.getPointColor() == null){
								    positionColor.setPointColor(getCompareColor(positionColor.getColor()));
								}
								Color cc = positionColor.getPointColor();
								if (cc != null) {
									if(positionColor.isEqual() && cc.equals(c)){
										sb.append("在(" + ((int) point.getX()) + "," + ((int) point.getY()) + ")颜色等于捕获的颜色(" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + ")，比较的颜色" + cc.getRed() + "," + cc.getGreen() + "," + cc.getBlue()).append("\n");
										changed = true;
									} else if(!positionColor.isEqual() && !cc.equals(c)){
										sb.append("在(" + ((int) point.getX()) + "," + ((int) point.getY()) + ")颜色发生变化，捕获的颜色(" + c.getRed() + "," + c.getGreen() + "," + c.getBlue() + ")，比较的颜色" + cc.getRed() + "," + cc.getGreen() + "," + cc.getBlue()).append("\n");
                                        sb.append("未匹配的次数"+positionColor.getNotMatchedTimes()+"\n");
										changed = true;
										// 只有比较不同颜色时才增加，通知的图标不活动隐藏了，点会偏移
                                        // 而且颜色不闪烁才增加
                                        if (positionColor.getPreColor() != null
                                                && positionColor.getPreColor().equals(positionColor.getCurColor())) {
                                            positionColor.setNotMatchedTimes(positionColor.getNotMatchedTimes() + 1);
                                        } else {
                                            positionColor.setNotMatchedTimes(0);
                                        }
                                        if (positionColor.getNotMatchedTimes() > positionColor.getMaxNotMatchedTimes()) {
                                            // 连续不相同，说明点偏移了
                                            positionColor.setPointColor(positionColor.getCurColor());
                                            scanColorInRange(robot,positionColor, point, sb);
                                            positionColor.setNotMatchedTimes(0);
                                        }
									} else {
										sb.append("在(" + ((int) point.getX()) + "," + ((int) point.getY()) + ")捕获的颜色" + c.getRed() + "," + c.getGreen() + "," + c.getBlue()+", 没有变化").append("\n");
										Color oldColor = getCompareColor(positionColor.getColor());
                                        if (!positionColor.isEqual() && !cc.equals(oldColor)) {
                                            sb.append("未匹配的次数"+positionColor.getNotMatchedTimes()+"\n");
                                            positionColor.setNotMatchedTimes(positionColor.getNotMatchedTimes() + 1);
                                            sb.append("配置的颜色" + oldColor.getRed() + "," + oldColor.getGreen() + "," + oldColor.getBlue()).append(
                                                    "\n");
                                            if (positionColor.getNotMatchedTimes() > positionColor.getMaxNotMatchedTimes()) {
                                                // 和原色不同，重新找一下
                                                scanColorInRange(robot, positionColor, point,sb);
                                                positionColor.setNotMatchedTimes(0);
                                            }
                                        }
									}
								} else {
									sb.append("在(" + ((int) point.getX()) + "," + ((int) point.getY()) + ")捕获的颜色" + c.getRed() + "," + c.getGreen() + "," + c.getBlue()+", 没有配置比较颜色").append("\n");
								}
							} else {
								sb.append("没有配置取点坐标").append("\n");
							}
						}
					}
				} catch (Exception e) {
					sb.append("\n").append(e.getMessage());
				}
				sendMessage(changed);
				getJTextArea().setText(sb.toString());
			}
		}, 500, 1000);
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
				@Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
					properties.setProperty("server", getServerTxt().getText());
					try {
						propFile.getParentFile().mkdirs();
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
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
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
					jLabel.setText(sdf.format(new Date()) + " 已连接到:"+ getServerTxt().getText());
					getServerTxt().setEditable(false);
				} catch (Exception e) {
					jLabel.setText(sdf.format(new Date()) + " 发送异常：" + e.getMessage());
				}
			} else {
				try {
					CommandSender sender = new SessionCommandSender(connector.getManagedSessions().values().iterator().next());
					if(changed){
						sender.sendCommand("new");
					} else {
						sender.sendCommand("none");
					}
					jLabel.setText(sdf.format(new Date()) + " 已发送消息到:"+ getServerTxt().getText());
				} catch (Exception e) {
					jLabel.setText(sdf.format(new Date()) + " 发送异常：" + e.getMessage());
				}
			}
		}
	}

	private void scanColorInRange(Robot robot, PositionColor positionColor, Point point, StringBuilder sb) {
        Color oldColor = getCompareColor(positionColor.getColor());
        if (!oldColor.equals(positionColor.getPointColor())) {
            // 点不同了，改变x向两边查找
            if (positionColor.getScanRange() <= 0) {
                positionColor.setScanRange(100);
            }
            for (int i = 1; i <= positionColor.getScanRange(); i++) {
                Point rightPoint = new Point((int) point.getX() + i, (int) point.getY());
                Color rightColor = robot.getPixelColor((int) rightPoint.getX(), (int) rightPoint.getY());
                if (oldColor.equals(rightColor)) {
                    positionColor.setPostionPoint(rightPoint);
                    return;
                }
                Point leftPoint = new Point((int) point.getX() - i, (int) point.getY());
                Color leftColor = robot.getPixelColor((int) leftPoint.getX(), (int) leftPoint.getY());
                if (oldColor.equals(leftColor)) {
                    positionColor.setPostionPoint(leftPoint);
                    return;
                }
            }
            sb.append("未找到颜色" + oldColor.getRed() + "," + oldColor.getGreen() + "," + oldColor.getBlue()+", 范围"+positionColor.getScanRange()).append("\n");
        }
    }

    /**
	 * Launches this application
	 */
	public static void main(final String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
            public void run() {
				NotifyClient application = new NotifyClient();
				application.init();
				if (args != null) {
					for (String string : args) {
						if ("-m".equals(string)) {
							application.getJFrame().setState(Frame.ICONIFIED);
						}
					}
				}
				if (application.getJFrame().getState() != Frame.ICONIFIED) {
					application.getJFrame().setVisible(true);
				}
			}
		});
	}
}
