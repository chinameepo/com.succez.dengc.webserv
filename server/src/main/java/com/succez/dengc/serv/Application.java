package com.succez.dengc.serv;
/**
 *All right resrvered esensoft(2011)
 * @author  邓超   deng.369@gmail.com
 * @version 1.0,创建时间：2011-8-8 上午09:34:41
 * @since   jdk1.5
 * 运行服务器
 */
public class Application {
	public static void main(String[] args) {
		Servers serve = new Servers(8066);
				serve.start();
	}
}
