//客户端
package org.westos.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

import org.westos.util.InputUtil;
import org.westos.util.TimeUtil;

/**
 * 客户端消息接收线程
 * 
 * @author Administrator
 *
 */
class ClientRoomThread extends Thread {

	/**
	 * 当前用户的Socket通道内的输入流
	 */
	private InputStream in;

	public ClientRoomThread(InputStream in) {
		super();
		this.in = in;
	}

	@Override
	public void run() {
		try {
			while (true) {
				// 读取服务器消息
				byte[] by = new byte[1024];
				int len = in.read(by);
				String msg = new String(by, 0, len);
				// 拆分消息 发送者:消息内容:功能标记:系统时间
				String[] msgs = msg.split(":");
				// 发送者
				String sender = msgs[0];
				// 消息内容
				String msgContent = msgs[1];
				// 标记
				int msgType = Integer.parseInt(msgs[2]);
				// 时间
				long time = Long.parseLong(msgs[3]);
				// 通过工具类解码时间
				String timeStr = TimeUtil.changeMils2Date(time, "HH:mm:ss");
				// 展示从服务器收到的消息
				switch (msgType) {
				// 私聊
				case Configs.MSG_PRIVATE:
					System.out.println(timeStr);
					System.out.println(sender + "对你说:" + msgContent);
					break;
				// 上线提醒
				case Configs.MSG_ONLINE:
					System.out.println(timeStr);
					System.out.println(sender + ":" + msgContent);
					break;
				// 公聊
				case Configs.MSG_PUBLIC:
					System.out.println(timeStr);
					System.out.println(sender + "对大家说" + msgContent);
					break;
				//在线列表
				case Configs.MSG_ONLIST:
					System.out.println(timeStr);
					System.out.println("当前在线用户:");
					System.out.println(msgContent);
				default:
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
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
 * 客户端主程序
 * 
 * @author Administrator
 *
 */
public class ClientRoom {

	private static OutputStream out;
	private static InputStream in;
	private static Scanner sc;

	public static void main(String[] args) {
		try {
			// 创建客户端Socket对象
			@SuppressWarnings("resource")
			Socket s = new Socket("192.168.10.89", 3344);
			// 获取当前用户的流
			out = s.getOutputStream();
			in = s.getInputStream();
			// 键盘录入
			sc = new Scanner(System.in);
			// 注册
			while (true) {
				System.out.println("请输入注册使用的用户名:");
				String username = sc.nextLine();
				// 用户名上传服务器
				out.write(username.getBytes());
				// 接收服务器的反馈
				byte[] bys = new byte[1024];
				int len = in.read(bys);
				String fk = new String(bys, 0, len);
				// 判断反馈信息
				if ("yes".equals(fk)) {
					System.out.println("注册成功");
					break;
				} else if ("no".equals(fk)) {
					System.out.println("用户名已存在请重新输入");
				}
			}
			// 启动客户端接收消息线程
			ClientRoomThread crt = new ClientRoomThread(in);
			crt.start();
			// 功能选择
			while (true) {
				System.out.println("请选择你需要的功能:");
				System.out.println("1 私聊, 2 公聊, 3 在线列表 , 4 退出");
				// 利用工具类获取用户输入,过滤为只能输入数字
				int num = InputUtil.inputIntType(new Scanner(System.in));
				// 根据输入执行任务
				switch (num) {
				case 1:
					// 私聊
					privateTalk();
					break;
				case 2:
					//公聊
					publicTalk();
					break;
				case 3:
					//在线列表
					getOnList();
					break;
				case 4:
					//退出
					
					break;

				default:
					System.out.println("输入有误请重新选择");
					break;
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**获取在线列表
	 * @throws IOException
	 */
	private static void getOnList() throws IOException {
		//消息格式:接收者:消息内容:消息类型
		String msg = "null"+":"+"null"+":"+Configs.MSG_ONLIST ;
		//上传服务器
		out.write(msg.getBytes());
	}

	/**进入公聊模式
	 * @throws IOException
	 */
	private static void publicTalk() throws IOException {
		while (true) {
			System.out.println("您当前处于公聊模式");
			System.out.println("消息格式为->消息内容	-q 退出当前模式");
			String msg = sc.nextLine();
			if ("-q".equals(msg)) {
				break;
			}
			//输入内容转化为服务器接收格式	接收者:消息内容:消息类型
			msg="null"+":"+msg+":"+Configs.MSG_PUBLIC;
			//上传服务器
			out.write(msg.getBytes());
		}
	}

	/**进入私聊模式
	 * @throws IOException
	 */
	private static void privateTalk() throws IOException {
		while (true) {
			// 格式 接收者:消息内容:消息类型
			System.out.println("当前您处于私聊模式");
			System.out.println("消息格式->	接收者:消息内容  -q 退出当前模式 ");
			//输入消息
			String msg = sc.nextLine();
			if ("-q".equals(msg)) {
				break;
			}
			//添加标记信息为私聊
			msg=msg+":"+Configs.MSG_PRIVATE;
			//发送到服务器
			out.write(msg.getBytes());
		}
	}

}
