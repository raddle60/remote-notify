package com.raddle.notify.remote;

import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.net.InetSocketAddress;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import com.raddle.nio.codec.impl.HessianCodec;
import com.raddle.nio.mina.cmd.handler.AbstractCommandHandler;
import com.raddle.nio.mina.codec.ChainCodecFactory;

public class NotifyServer {
	static {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
		}
	}
	private JFrame jFrame = null;
	private JDesktopPane jDesktopPane = null;
	private JButton startBtn = null;
	private JButton stopBtn = null;
	private JScrollPane jScrollPane = null;
	private JTextArea messageTxt = null;
	private JLabel jLabel = null;
	private JTextField portTxt = null;
	private IoAcceptor acceptor = new NioSocketAcceptor();  
	private BufferedImage noMsgImage = null;  //  @jve:decl-index=0:
	private BufferedImage newMsgImage = null; 
	private TrayIcon trayIcon = null;  //  @jve:decl-index=0:
	private long crossInc = 0;
	private boolean hasMsg = false;
	private Object waitingMsg = new Object();  //  @jve:decl-index=0:
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
			jFrame.setSize(300, 200);
			jFrame.setContentPane(getJDesktopPane());
			jFrame.setTitle("通知服务端");
			jFrame.addWindowListener(new java.awt.event.WindowAdapter() {

				@Override
				public void windowClosed(WindowEvent e) {
					acceptor.unbind();
					acceptor.dispose();
					super.windowClosed(e);
				} 
			});
		}
		return jFrame;
	}

	private void init(){
		acceptor.getSessionConfig().setReaderIdleTime(10);// 10秒沒收到数据就超时
		ChainCodecFactory chainCodecFactory = new ChainCodecFactory();
		chainCodecFactory.addFirst(new HessianCodec());
		acceptor.getFilterChain().addLast("codec", new ProtocolCodecFilter(chainCodecFactory));
		// 处理接收的命令和响应
		acceptor.setHandler(new AbstractCommandHandler() {

			@Override
			protected Object processCommand(Object command) {
				if ("new".equals(command)) {
					jFrame.setTitle("通知服务端 - 有新的消息");
					getMessageTxt().setText("有新的消息");
					trayIcon.setToolTip("有新的消息");
					hasMsg = true;
					synchronized (waitingMsg) {
						waitingMsg.notify();
					}
				} else if("none".equals(command)) {
					jFrame.setTitle("通知服务端 - 沒有新消息");
					getMessageTxt().setText("沒有新消息");
					if (trayIcon.getImage() != noMsgImage) {
						trayIcon.setImage(noMsgImage);
					}
					trayIcon.setToolTip("沒有新消息");
					hasMsg = false;
				}
				return null;
			}

			@Override
			public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
				session.close(true);
			}

			@Override
			public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
				session.close(true);
			}

			@Override
			protected String getExecuteQueue(Object command) {
				return null;
			}

			@Override
			public void sessionClosed(IoSession session) throws Exception {
				hasMsg = true;//断开连接说明有异常情况
			}

		});
		getStopBtn().setEnabled(false);
		/////// tray
		try {
			noMsgImage = ImageIO.read(NotifyServer.class.getResourceAsStream("/man.png"));
			newMsgImage = ImageIO.read(NotifyServer.class.getResourceAsStream("/card.png"));
			SystemTray systemTray = SystemTray.getSystemTray();
			trayIcon = new TrayIcon(noMsgImage, "没有新的消息");
			systemTray.add(trayIcon);
		} catch (Exception e) {
			getMessageTxt().setText(e.getMessage());
		}
		trayIcon.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {// 双击托盘窗口再现
					if(jFrame.isVisible()){
						jFrame.setVisible(false);
					} else {
						jFrame.setVisible(true);
					}
				}
			}
		});
		////// event 
		jFrame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowIconified(WindowEvent e) {
				jFrame.setVisible(false);
				super.windowIconified(e);
			}
			
		});
		//////////
		Thread thread = new Thread(){

			@Override
			public void run() {
				while(true){
					if(hasMsg){
						crossInc ++;
						if(crossInc % 2 == 0){
							trayIcon.setImage(noMsgImage);
						} else {
							trayIcon.setImage(newMsgImage);
						}
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							throw new RuntimeException(e.getMessage(), e);
						}
					} else {
						synchronized (waitingMsg) {
							try {
								waitingMsg.wait();
							} catch (InterruptedException e) {
								throw new RuntimeException(e.getMessage(), e);
							}
						}
					}
				}
			}
			
		};
		thread.setDaemon(true);
		thread.start();
	}
	/**
	 * This method initializes jDesktopPane	
	 * 	
	 * @return javax.swing.JDesktopPane	
	 */
	private JDesktopPane getJDesktopPane() {
		if (jDesktopPane == null) {
			jLabel = new JLabel();
			jLabel.setBounds(new Rectangle(15, 14, 38, 37));
			jLabel.setText("Port");
			jDesktopPane = new JDesktopPane();
			jDesktopPane.add(getStartBtn(), null);
			jDesktopPane.add(getStopBtn(), null);
			jDesktopPane.add(getJScrollPane(), null);
			jDesktopPane.add(jLabel, null);
			jDesktopPane.add(getPortTxt(), null);
		}
		return jDesktopPane;
	}

	/**
	 * This method initializes startBtn	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getStartBtn() {
		if (startBtn == null) {
			startBtn = new JButton();
			startBtn.setBounds(new Rectangle(107, 15, 77, 33));
			startBtn.setText("启动");
			startBtn.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					try {
						acceptor.bind(new InetSocketAddress(Integer.parseInt(getPortTxt().getText())));
						getMessageTxt().setText("监听在端口"+getPortTxt().getText());
						getStopBtn().setEnabled(true);
						getStartBtn().setEnabled(false);
					} catch (Exception e1) {
						getMessageTxt().setText(e1.getMessage());
					}
				}
			});
		}
		return startBtn;
	}

	/**
	 * This method initializes stopBtn	
	 * 	
	 * @return javax.swing.JButton	
	 */
	private JButton getStopBtn() {
		if (stopBtn == null) {
			stopBtn = new JButton();
			stopBtn.setBounds(new Rectangle(196, 14, 75, 32));
			stopBtn.setText("停止");
			stopBtn.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent e) {
					acceptor.unbind();
					getStopBtn().setEnabled(false);
					getStartBtn().setEnabled(true);
				}
			});
		}
		return stopBtn;
	}

	/**
	 * This method initializes jScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */
	private JScrollPane getJScrollPane() {
		if (jScrollPane == null) {
			jScrollPane = new JScrollPane();
			jScrollPane.setBounds(new Rectangle(15, 61, 267, 104));
			jScrollPane.setViewportView(getMessageTxt());
		}
		return jScrollPane;
	}

	/**
	 * This method initializes jTextArea	
	 * 	
	 * @return javax.swing.JTextArea	
	 */
	private JTextArea getMessageTxt() {
		if (messageTxt == null) {
			messageTxt = new JTextArea();
		}
		return messageTxt;
	}

	/**
	 * This method initializes portTxt	
	 * 	
	 * @return javax.swing.JTextField	
	 */
	private JTextField getPortTxt() {
		if (portTxt == null) {
			portTxt = new JTextField();
			portTxt.setBounds(new Rectangle(54, 14, 50, 36));
			portTxt.setText("12111");
		}
		return portTxt;
	}

	/**
	 * Launches this application
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				NotifyServer application = new NotifyServer();
				application.getJFrame();
				application.init();
				application.getJFrame().setVisible(true);
			}
		});
	}

}
