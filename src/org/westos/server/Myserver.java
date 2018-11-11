package org.westos.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Set;

/**
 * 保存用户用户名的线程
 * 
 * @author Administrator
 *
 */
class SaveUserThread extends Thread {
	/** 用户的Socket对象 */
	private Socket s;
	/** 储存用户名和用户Socket对象的HashMap */
	private HashMap<String, Socket> hm;
	/** 当前访问的用户的用户名 */
	private String username;

	public SaveUserThread(Socket s, HashMap<String, Socket> hm) {
		super();
		this.s = s;
		this.hm = hm;
	}

	@Override
	public void run() {
		try {
			// 获取通道内的流
			InputStream in = s.getInputStream();
			OutputStream out = s.getOutputStream();
			// 储存用户名
			while (true) {
				byte[] bys = new byte[1024];
				int len = in.read(bys);
				username = new String(bys, 0, len);
				// 如果HashMap集合的键(k)中没有当前用户输入的username
				if (!hm.containsKey(username)) {
					// 添加到HashMap
					hm.put(username, s);
					// 反馈给用户yes表示成功
					out.write("yes".getBytes());
					System.out.println("注册:" + username);
					break;
				} else {
					out.write("no".getBytes());
				}
			}
			/*
			 * 上线提醒
			 */
			// 获取HashMap集合中的键(k)的集合
			Set<String> keySet = hm.keySet();
			// 遍历
			for (String key : keySet) {
				// 排除自己
				if (key.equals(username)) {
					continue;
				}
				// 通过键找值获取除当前用户外的每个用户的sock
				Socket sock = hm.get(key);
				// 获取每个用户的输出流
				OutputStream os = sock.getOutputStream();
				// 反馈给每个用户的信息,时间统一为服务器时间
				String zf = username + ":" + "上线了" + ":" + Configs.MSG_ONLINE + ":" + System.currentTimeMillis();
				// 发送给除当前用户外的每个用户
				os.write(zf.getBytes());
			}
			// 开启聊天功能
			new ServerThread(s, hm, username).start();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

/**
 * 转发消息,聊天线程
 * 
 * @author Administrator
 *
 */
class ServerThread extends Thread {
	/** 用户的Socket对象 */
	private Socket s;
	/** 储存用户名和用户Socket对象的HashMap */
	private HashMap<String, Socket> hm;
	/** 当前访问的用户的用户名 */
	private String username;

	/**
	 * 聊天线程的构造方法
	 * 
	 * @param s
	 *            用户的Socket对象
	 * @param hm
	 *            储存用户名和用户Socket对象的HashMap
	 * @param username
	 *            当前访问的用户的用户名
	 */
	public ServerThread(Socket s, HashMap<String, Socket> hm, String username) {
		super();
		this.s = s;
		this.hm = hm;
		this.username = username;
	}

	@Override
	public void run() {
		try {
			// 获取当前用户通道内的流
			InputStream in = s.getInputStream();
			OutputStream out = s.getOutputStream();
			// 监听读取
			while (true) {
				byte[] bys = new byte[1024];
				int len = in.read(bys);
				// msgstr客户端发来的消息 接收者:消息内容:消息类型标记
				String msgstr = new String(bys, 0, len);
				// 分割消息
				String[] msg = msgstr.split(":");
				// 接收者
				String receiver = msg[0];
				// 消息内容
				String msgContent = msg[1];
				// 消息类型标记
				int msgType = Integer.parseInt(msg[2]);
				// 解析标记信息输出到服务器控制台
				typeFun(receiver, msgContent, msgType);
				// 将收到的消息以服务器格式重新组装 发送者:消息内容:消息类型:时间
				// 当前时间
				long time = System.currentTimeMillis();
				// 根据消息类型标记做出不同的功能
				// 私聊
				if (msgType == Configs.MSG_PRIVATE) {
					// 通过接受者找对应的Socket对象
					Socket soc = hm.get(receiver);
					// 判断请求的用户是否存在
					if (soc == null) {
						// 如果不存在给用户发送反馈信息
						String fk = "服务器" + ":" + "输入用户" + receiver + "不存在" + ":" + Configs.MSG_PRIVATE + ":" + time;
						out.write(fk.getBytes());
					} else {
						// 转发给目标用户的消息格式 发送者:消息内容:消息类型:时间
						String zf = username + ":" + msgContent + ":" + msgType + ":" + time;
						// 发送给目标用户
						soc.getOutputStream().write(zf.getBytes());
					}
				}
				// 公聊
				else if (msgType == Configs.MSG_PUBLIC) {
					// 遍历HashMap
					Set<String> keySet = hm.keySet();
					for (String k : keySet) {
						// 排除当前用户
						if (k.equals(username)) {
							continue;
						}
						// 获取除当前用户外的所有用户的Socket
						Socket soc = hm.get(k);
						OutputStream os = soc.getOutputStream();
						// 按格式发送 发送者(当前用户):消息内容:消息类型:系统时间
						String zf = username + ":" + msgContent + ":" + Configs.MSG_PUBLIC + ":" + time;
						os.write(zf.getBytes());
					}
				}
				//在线列表
				else if (msgType==Configs.MSG_ONLIST) {
					StringBuffer sb=new StringBuffer();
					int i=1;
					//遍历用户集合
					Set<String> keySet = hm.keySet();
					for (String key : keySet) {
						//排除自己
						if (key.equals(username)) {
							continue;
						}
						//拼接在线列表
						sb.append(i++).append(",").append(key).append("\n");
						//发送给当前访问客户端
						String zf= username+ ":" + sb.toString() + ":" + Configs.MSG_ONLIST + ":" + time;
						out.write(zf.getBytes());
					}
					
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 解析功能标记并输出到服务器控制台
	 * 
	 * @param receiver
	 * @param msgContent
	 * @param msgType
	 */
	private void typeFun(String receiver, String msgContent, int msgType) {
		String fun;
		switch (msgType) {

		case Configs.MSG_PRIVATE:
			fun = "私聊";
			break;
		case Configs.MSG_PUBLIC:
			fun = "公聊";
			break;
		case Configs.MSG_ONLIST:
			fun = "在线列表";
			break;
		default:
			fun = "未定义";
			break;
		}
		System.out.println(username + "->" + " 接收:" + receiver + " 内容:" + msgContent + " 功能:" + fun);
	}
}

/**
 * 消息功能的标记
 * 
 * @author Administrator
 *
 */
class Configs {
	/**
	 * 私聊标记
	 */
	public static final int MSG_PRIVATE = 100;
	/**
	 * 公聊标记
	 */
	public static final int MSG_PUBLIC = 200;
	/**
	 * 上线提醒标记
	 */
	public static final int MSG_ONLINE = 300;
	/**
	 * 在线列表
	 */
	public static final int MSG_ONLIST = 400;

	/**
	 * 退出
	 */
	public static final int MSG_EXIT = 500;
}

/**
 * 服务器主程序
 * 
 * @author Administrator
 *
 */
public class Myserver {

	public static void main(String[] args) {
		try {
			// 创建服务器端的ServerSocket
			@SuppressWarnings("resource")
			ServerSocket sst = new ServerSocket(3344);
			// 储存用户名和用户Socket的集合
			HashMap<String, Socket> hm = new HashMap<String, Socket>();
			System.out.println("服务器已开启");
			int i = 1;
			while (true) {
				// 监听客户端连接
				Socket s = sst.accept();
				// 服务器显示
				System.out.println("第" + i++ + "个客户端登录");
				// 用户名效验并注册
				SaveUserThread st = new SaveUserThread(s, hm);
				st.start();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
