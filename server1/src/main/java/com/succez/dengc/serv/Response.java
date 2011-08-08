package com.succez.dengc.serv;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *All right resrvered esensoft(2011)
 * @author  邓超   deng.369@gmail.com
 * @version 1.0,创建时间：2011-8-2 下午07:12:36
 * @since   jdk1.6
 * 浏览器应答类，通过获得的socket对象，利用socket对象的IO流和浏览器通信。
 */
public class Response implements Runnable {
	/**
	 * 这个socket对象必须要从Serve监听对象传过来，和浏览器交互的IO流都是通过它来建立，
	 * 通过对象的输入流来获取url，通过对象的输出流来向浏览器返回内容。 不可缺少
	 */
	private Socket socket;
	/**
	 *  服务器的默认根目录，从Serve对象中获得 ，可以缺省
	 * */
	private String root;
    private Logger logger = LoggerFactory.getLogger(Response.class);
   
	public Response(Socket response, String root) {
		this.socket = response;
		this.root = root;
	} 
	/**
	 * C:系统盘，最好不要将根目录设在这里
	 * */
	public Response(Socket response) {
		this(response, "c:\\");
	}
	
	/**
	 * 核心方法，规定该线程的执行内容，通过浏览器的输入流得到请求报文头，从中截取文件Url,
	 * 如果文件存在，读取文件，将内容传输到浏览器。不存在，返回404文件
	 * 
	 * @exception IOException
	 * */
	public void run() {
			OutputStream out = null;
			InputStream socketiStream = null;
			try {
				socketiStream = socket.getInputStream();
				String httpRequestHead = getHttpHead(socketiStream);
				String sourcePath = root+getUrl(httpRequestHead);;
				try {
					out = socket.getOutputStream();
					fileToBrowser(sourcePath, out);
				} catch (IOException e) {
					logger.error("浏览器输出流对象新建的时候出错！程序终止！来自方法：【run】");
					return;
				}
			} catch (IOException e) {
				logger.error("程序运行中出现错误，程序终止！来自方法：【run】");
				return;
			} finally {
				try {
					out.close();
					socketiStream.close();
					socket.close();
				} catch (IOException e) {
					logger.error("文件流在关闭的时候出现错误！不能正常关闭。自方法：【run】");
					return;
				}
			}
	}
	/**
	 * 获取请求报文所有内容。
	 * @param inputStream
	 * @return String
	 */
	public String getHttpHead(InputStream inputStream) {
		if(inputStream==null)
			return "";
		StringBuilder requestStr = new StringBuilder();
		try {
			int len=-1;
			byte[] buff =new byte[1024];
			do{
				len=inputStream.read(buff);
				if(len!=-1)
				{
					requestStr.append(new String(buff,0,len,"UTF-8")).append('\n');
				}
			}while(len==1024);
		} catch (IOException e) {
			logger.error("读取请求报文过程中出错！来自方法：【 getHttpHead】");
			return "";
		}
		logger.info("请求的报文头是：/n{}",requestStr.toString());
		return requestStr.toString();
	}
	/**
	 * @param requestString
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public String getUrl(String requestString)throws UnsupportedEncodingException {
		if ("".equals(requestString))
			return "";
		/* 截断报文，获取url。请求报文第一行的格式都是 GET /urlcontent**** HTTP/1.X。*/
		int indexOfHTTP =requestString.lastIndexOf("HTTP/1.");
		if(indexOfHTTP==-1)
			return "";
		String url  = requestString.substring(5,indexOfHTTP).trim();
		/* 如果对方输入的页面是空的，例如http://localhost:8080/,跳至首页 */
		if ("".equals(url))
		{
			url = "index.html";
			return url;
		}
		/*浏览器处理url要用到UTF-8编码，处理空格后会变成“%20”或者“+”,要进行解码还原*/
		url =URLDecoder.decode(url,"UTF-8");
		logger.info("文件名是：{}", url);
		return url;
	}
	
	/**
	 * 读取文件，写入浏览器。 如果url指定的文件存在，传到浏览器。如果不存在，就在浏览器上显示404页面
	 * 这里可能出现的问题：如果用户指定的404页面不存在， 那么程序就会陷入死循环。
	 * InputStream会抛出FileNotFoundException异常，is.read(buf)会抛出IOException
	 * @param sourceName
	 *            ，要读取文件的文件名
	 * @param out
	 *            将文件写入浏览器的输出流
	 * */
	public void fileToBrowser(String sourceName, OutputStream out) {
		if(out==null)
			return;
		File file = new File(sourceName);
		if (file.exists()&&file.isFile()) {
			sendHead(sourceName, out);
			sendFile(file,out);
		} else {
			logger.error("程序要找的文件【{}】找磁盘上找不到！将会用404页面来替代这个文件返回！来自方法：【fileToBrowser】", sourceName);
             /* 注意这是递归调用，如果说这个404文件不存在，就会陷入死循环*/
			fileToBrowser(root+"404.html", out);
		}
		}
	
	/**
	 * 发送报文头到一个ouputStrem对象，就是socket的outputStream对象
	 * 这代码这么写的前提是：参数都是有意义的，都不为空。
	 * @param sourceName
	 * @param out
	 * @param file
	 * @throws IOException
	 */
	public void sendHead(String sourceName, OutputStream out){	
		String head = "HTTP/1.1 200 OK" + "\n"
				+ "Date: Thu, 21 Jul 2011 01:45:42 GMT" + "\n"
				+ "Content-Length: " + (new File(sourceName)).length() + "\n"
				+ "Content-Type: "+getMIMEtype(sourceName) + "\n"
				+ "Cache-Control: private" + "\n"
				+ "Expires: Thu, 21 Jul 2011 01:45:42 GMT" + "\n"
				+ "Connection: Keep-Alive" + "\n" + "\n";
		try {
			out.write(head.getBytes());
		} catch (IOException e) {
			logger.error("传送文件{}前，发送报文头失败！来自方法【sendHead】",sourceName);
			return;
		}
	}

	/**
	 * 发送文件到一个ouputStrem对象，就是socket的outputStream对象
	 * 这代码这么写的前提是：这三个参数都是有意义的，都不为空。
	 * @param out
	 * @param file
	 */
	public void sendFile( File file,OutputStream out) {
		InputStream is =null;
		try {
			is = (InputStream)(new FileInputStream(file));
			byte[] buf = new byte[1024];
			int len;
			while ((len = is.read(buf))!= -1) {
				out.write(buf, 0, len);
			}
			out.flush();
		} catch (IOException e) {
			logger.error("文件【{}】读取、写入浏览器中出现错误！来自方法:【sendFile】",file.toString());
			return;
		} finally {
			try {
				is.close();
			} catch (IOException e2) {
				logger.error("文件{}的读取流无法正常关闭！来自方法:【sendFile】",file.toString());
				return;
			}
		}
	}
	
	/**
	 * 根据文件名的后缀类型来确定返回类型。因为执行这段代码之前， 已经知道这个文件肯定存在，所以肯定是有后缀的
	 * 决定应答报文头“Content-Type”那一行的内容， 例如如果是aa.jpg，返回就是"Content-Type: image/jpeg"
	 * 要注意，文件名中包含了“”/\：*<>等符号都是不合法的。这个里面的判断比较多。
	 * @return String
	 * @param String sourceString ，url的字符串
	 *            
	 */
	public String getMIMEtype(String sourceString) {
		if(sourceString==null||"".equals(sourceString))
			return "text/html;charset=gb2312";
		/*先把句号的位置取出来，是为了防止sourceString中没有“.”号，返回-1，
		 * 从而造成substring值溢出的情况发生。*/
		int indexOfDot =sourceString.lastIndexOf('.');
		if(indexOfDot==-1)
			return "text/html;charset=gb2312";
		String type=sourceString.substring(indexOfDot);
		Map<String, String> map= new HashMap<String, String>();
		initMap("src/main/java/com/succez/dengc/serv/mimeMap",map);
		String returnType =map.get(type);
		if(returnType==null)
			return "text/html;charset=gb2312";
		return returnType;
	}
	/**
	 *对map对象进行初始化，把一个文件里面的hash关系读取，放入map中
	 *@param String sourcFile map关系保存的文件所在     ' '  '.'  "_" '_' '-'
     *@param Map<String,String> map存放读取出来的关系印射
	 * */
	public void initMap(String sourcFile,Map<String,String> map)
	{   
		BufferedReader in =null;
		try {
			in = new BufferedReader(new FileReader(sourcFile));
			String[] mapString = new String[2];
			String aline;
			try {
				do {
				   aline =in.readLine();
				   if(aline!=null)
				   {  
					   /*String.splite函数第二个参数是规定按照表达式分割n-1次，所以这里是按照==号分割一次
					    * 当然也有可能管理员疏忽，没用加上==号，那么印射值就返回null，接着mime就
					    * 返回text/html类型*/
					   mapString =aline.split("==",2);
					   map.put(mapString[0].trim(), mapString[1].trim());
				   }
				   } while (aline!=null);	
			} catch (IOException e) {
				logger.error("在读取map的印射关系的时候出错，文件名是{}.来自【initMap】",sourcFile);
				return;
			}	                           	
		} catch (FileNotFoundException e) {
			logger.error("map的印射关系文件名{}找不到，请检查您的路径是否正确。来自【initMap】",sourcFile);
			return;
		}finally
		{
			try {
				in.close();
			} catch (Exception e2) {
				logger.error("在关闭map的印射关系的文件出错，文件名是{}.来自【initMap】",sourcFile);
				return;
			}
		}
	}
}


