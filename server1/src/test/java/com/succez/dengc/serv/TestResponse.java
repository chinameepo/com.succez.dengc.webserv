package com.succez.dengc.serv;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.succez.dengc.serv.Response;;

/**
 * All right resrvered esensoft(2011)
 * 
 * @author 邓超 deng.369@gmail.com
 * @version 1.0,创建时间：2011-8-3 上午08:44:10
 * @since jdk1.5 
 * 对response类的测试类，请注意。如果你要用自己的文件拖进来做测试，请做好备份。
 */
public class TestResponse {
	@Test
	public final void testGetHttpHead() throws FileNotFoundException
	{
		testGetHttpheadSub("GET /fw48*()()?>LL: HTTP/1.1"+"\n","src/test/java/testfl/reqst-cmplx.txt");
		testGetHttpheadSub("GET /c%20s%20s.txt HTTP/1.1"+"\n","src/test/java/testfl/reqst-dec%.txt");
		testGetHttpheadSub("GET /Temp/c+s+s.txt HTTP/1.1"+"\n","src/test/java/testfl/reqst-dec+.txt");
		testGetHttpheadSub("GET /Temp/test HTTP/1.1"+"\n","src/test/java/testfl/reqst-dir.txt");  
		testGetHttpheadSub("","src/test/java/testfl/reqst-empt.txt");
		testGetHttpheadSub("GET / HTTP/1.1"+"\n","src/test/java/testfl/reqst-home.txt");
	}
	/**
	 * testGetHttpHead()的辅助方法
	 * */
	public void testGetHttpheadSub(String except,String compareFilePath) throws FileNotFoundException
	{
		Response response = new Response(null);
		StringBuilder builder = new StringBuilder();
		File file = new File(compareFilePath);
		InputStream in = null;
		if (file.exists() && file.isFile()) {
			try {
				in = (InputStream) (new FileInputStream(file));
				builder.append(response.getHttpHead(in));
				assertEquals(except, builder.toString());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					in.close();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
		}
	}
	@Test
	public final void testGetUrl() throws UnsupportedEncodingException
	{
		Response response = new Response(null);
		assertEquals("aa.png", response.getUrl("GET /aa.png HTTP/1.1"));
		assertEquals("aa.png", response.getUrl("GET /aa.png HTTP/1.0"));
		assertEquals("aa.png", response.getUrl("GET /aa.png HTTP/1.0"+"keep alive"));
		assertEquals("index.html", response.getUrl("GET / HTTP/1.0"));
		assertEquals("a a .png", response.getUrl("GET /a%20a%20.png HTTP/1.0"));
		assertEquals("a a .png", response.getUrl("GET /a+a+.png HTTP/1.0"));
		assertEquals("", response.getUrl(""));
		
	}
	
	/**
	 * 具体的写入头文件、写入文件的，这个被测试方法的子方法已经测试过了
	 * 这个类主要是测试：1，错误的地址是否会返回404页面。2，应答报文头是否写进了文件
	 * 先使用bufferedreader的realine方法出报文头，再接着比较以后的内容。
	 * 不管是文本文件，还是图片，一视同仁
	 * @throws IOException
	 * */
	@Test
	public final void testFileToBrowser() throws IOException {
		testFileToBroSub("src/test/java/testfl/test.txt","src/test/java/generfl/test-all.txt");
		testFileToBroSub("src/test/java/testfl/test.html", "src/test/java/generfl/test-all.html");
		testFileToBroSub("src/test/java/testfl/aa.png", "src/test/java/generfl/aa-all.png");
		testFileToBroSub("src/test/java/testfl/bb.jpg", "src/test/java/generfl/bb-all.jpg");
		/*测试输入错误url被替换为404页面，有点难度，不过还是可以实现的。
		 *下面我先给个错误地址，不出意外，err.html就是c://4041.html加上报文头*/
		testFileToBroSub("fewfweij.fwe", "src/test/java/generfl/err.html");
		/*然后我让404页面也写入另外的文件*/
		testFileToBroSub("c://404.html", "src/test/java/generfl/404-all.html");
		/*接着对比两个写入的结果*/
		assertEquals(readFile("src/test/java/generfl/err.html"), readFile("src/test/java/generfl/404-all.html"));
	}
	/**
	 * 为了测试FileToBrowser(String source,OutputStream out)写的辅助测试方法
	 * 验证写入的报文头是否正确
	 * */
	public void checkHead(String source,BufferedReader resultFileReader,Response response )
	throws IOException{
		File file= new File(source);
		// 验证报文头是否正确
		assertEquals("HTTP/1.1 200 OK", resultFileReader.readLine());
		assertEquals("Date: Thu, 21 Jul 2011 01:45:42 GMT", resultFileReader.readLine());
		assertEquals("Content-Length: " + file.length(), resultFileReader.readLine());
		assertEquals("Content-Type: " + response.getMIMEtype(source),resultFileReader.readLine());
		assertEquals("Cache-Control: private", resultFileReader.readLine());
		assertEquals("Expires: Thu, 21 Jul 2011 01:45:42 GMT",resultFileReader.readLine());
		assertEquals("Connection: Keep-Alive", resultFileReader.readLine());
		assertEquals("", resultFileReader.readLine());
	}
	/**
	 * 为了测试FileToBrowser(String source,OutputStream out)写的辅助测试方法
	 * 验证从一个文件写入另一个文件的内容是否正确
	 * @param resultReader
	 * @param sourceReader
	 * @throws IOException
	 */
	public void checkConten(BufferedReader resultReader,BufferedReader sourceReader)
			 throws IOException {
		String sourceString ,outString;
		while (((sourceString = sourceReader.readLine()) != null)
				&&((outString = resultReader.readLine()) != null)) {
			assertEquals(sourceString, outString);
		}
	}
	/**
	 * 辅助测试fileToBrowser的方法，验证写入的报文头+内容是否正确。测试方法中，
	 * 我们只是需要传入两个文件的路径就可以完成测试。此处注意的是测试的文件c://404.html，
	 * 这个文件比较特殊，如果把它放入了工程目录下，文件会变大，导致测试部准确。所以必须
	 * 是测试它的原文件。
	 * @throws IOException
	 * */
	public void testFileToBroSub(String sourceName,String outPath) throws IOException
	{
		Response response = new Response(null);
		OutputStream out = null;
		BufferedReader resultFileReader = null;
		BufferedReader sourceFileReader = null;	
		File sourceFile = new File(sourceName);
		if(!sourceFile.exists()||!sourceFile.isFile())
			{
			sourceFile = new File("c://404.html");
			sourceName ="c://404.html";
			}
		try {
			out = (OutputStream)(new FileOutputStream(outPath));
			response.fileToBrowser(sourceName, out);
			sourceFileReader = new BufferedReader(new FileReader(sourceName));
			resultFileReader = new BufferedReader(new FileReader(outPath));
			checkHead(sourceName, resultFileReader, response);
			checkConten(resultFileReader, sourceFileReader);
		} finally {
			try {
				out.close();
				resultFileReader.close();
				sourceFileReader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	/**
	 * 确定这个被测试的方法的参数out,对象肯定不为空.url,file都是不为空的。查看发送的报文头是否正确！
	 * 
	 * @throws IOException
	 * */
	@Test
	public final void testSendHead() throws IOException {
		Response response = new Response(null);
		testSendHeadSub("src/test/java/testfl/test.txt","src/test/java/generfl/test-head.txt", response);
		testSendHeadSub("src/test/java/testfl/test.txt","src/test/java/generfl/test-head.txt",response);
		testSendHeadSub("src/test/java/testfl/test.html","src/test/java/generfl/test-head.txt",response);
		testSendHeadSub("src/test/java/testfl/aa.png", "src/test/java/generfl/aa-head.txt",response);
		testSendHeadSub("src/test/java/testfl/bb.jpg", "src/test/java/generfl/bb-head.txt",response);
	}
	/**
	 * 这个函数大部分功能就是函数checkHead（）的功能，这是因为参数不同才特意新建了函数。
	 * */
	public void testSendHeadSub(String sourceName,String outPath,Response response) throws IOException
	{
		OutputStream out = null;
		BufferedReader resultFileReader = null;
		//查看文本文件
		try {
			out = (OutputStream) (new FileOutputStream(outPath));
			response.sendHead(sourceName , out);
			resultFileReader = new BufferedReader(new FileReader(outPath));
			checkHead(sourceName, resultFileReader, response);
		} finally {
			try {
				out.close();
				resultFileReader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 我认为即便不是文本文档，也可以用字符串来对文件的内容进行比较 前面，待测试的方法已经验证，这个函数的out肯定是不为空的
	 * 
	 * @throws IOException
	 * */
	@Test
	public final void testSendFile() throws IOException {
		testSendFileSub("src/test/java/testfl/test.txt","src/test/java/generfl/test-result.txt");
		testSendFileSub("src/test/java/testfl/aa.png","src/test/java/generfl/aa-result.png");
		testSendFileSub("src/test/java/testfl/bb.jpg","src/test/java/testfl/bb-result.jpg");
		testSendFileSub("src/test/java/testfl/404.html","src/test/java/testfl/404-result.html");
		testSendFileSub("src/test/java/testfl/MAP.doc","src/test/java/testfl/MAP-result.doc");
	}
	/**
	 * 为了测试SendFile()写的辅助函数，
	 * 查看写入的文件是否很原来的文件内容一样*/
	public void testSendFileSub(String sourceName,String outPath) throws IOException
	{
		Response response = new Response(null);
		OutputStream out = null;
		try {
			out = (OutputStream)(new FileOutputStream(outPath));	
			File file = new File(sourceName);
			response.sendFile(file, out);
			assertEquals(readFile(sourceName),readFile(outPath));
		} finally {
			try {
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}	
	}
	
	/**
	 * GetMIMEtpye()的测试方法，目前不能解决的问题是，如果用户输入url中就包含了很多“”号，或者/n/t之类的转义字符 如何处理？
	 */
	@Test
	public final void testGetMIMEtpye() {
		Response response = new Response(null);
		// 图片类型
		assertEquals("image/jpeg", response.getMIMEtype("a!@#$!%^&*%HRa.jpg"));
		assertEquals("image/jpeg",response.getMIMEtype("a$&*@;';&##^@a.jpg"));
		assertEquals("image/jpeg",response.getMIMEtype("xW@@xFE$--=-=YYW%Q.jpeg"));		
		assertEquals("image/png",response.getMIMEtype("?pf%%%efw jo.png"));	
		assertEquals("image/gif",response.getMIMEtype("i@@22-=t4-+-5+  -.gif"));
		// 文字类型
		assertEquals("text/xml",response.getMIMEtype("!!@!@$FEAAEH　　.xml"));
		assertEquals("text/plain", response.getMIMEtype("sss....txt"));
		assertEquals("text/html;charset=gb2312",response.getMIMEtype("s. .t  x  t"));
		assertEquals("text/html;charset=gb2312",response.getMIMEtype("Test. j av  a "));
		assertEquals("text/plain", response.getMIMEtype("c s s s s---ss....c"));
		assertEquals("text/plain",response.getMIMEtype("fff ff fw@#$.cpp"));		
		assertEquals("text/plain", response.getMIMEtype("Test.java"));
		// 在后面留一些空格
		assertEquals("text/html;charset=gb2312",response.getMIMEtype("test.java  "));
		assertEquals("text/html;charset=gb2312", response.getMIMEtype("inde x .h "));
		// 在前面留一些空格
		assertEquals("text/plain",response.getMIMEtype("  Test.java"));
		assertEquals("text/plain", response.getMIMEtype("cpp_h)(942334).txt"));
		assertEquals("text/plain", response.getMIMEtype("index.h"));
		assertEquals("text/html;charset=gb2312",response.getMIMEtype("index.html"));
		assertEquals("text/html;charset=gb2312",response.getMIMEtype("index.htm"));	
		assertEquals("text/html;charset=gb2312",response.getMIMEtype("index. ht m l"));	
		// 空类型
		assertEquals("text/html;charset=gb2312", response.getMIMEtype(""));
		assertEquals("text/html;charset=gb2312", response.getMIMEtype("abc"));
		assertEquals("text/html;charset=gb2312", response.getMIMEtype(null));
	}
	
	/**
	 * 为了测试方法服务的方法，读取一个文件的内容，不管是文件还是图片，我们都用字符串返回 。
	 * 不设置成同步方法的话 ，文件可能会在读取过程中损坏
	 * 
	 * @param String
	 *            filePath 文件的路径
	 */
	public synchronized String readFile(String filePath) {
		InputStream in = null;
		StringBuilder builder = new StringBuilder();
		File file = new File(filePath);
		if (file.exists() && file.isFile()) {
			try {
				in = (InputStream) (new FileInputStream(file));
				int len;
				byte[] buffer = new byte[512];
				while ((len = in.read(buffer)) != -1) {
					builder.append(new String(buffer, 0, len, "UTF-8"));
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					in.close();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
		}
		return builder.toString();
	}
	/**
	 * InitMap的测试方法
	 * */
	@Test
	public final void testInitMap()
	{
		Response response = new Response(null);
		String sourceFile ="src/main/java/com/succez/dengc/serv/mimeMap";
		Map<String, String> map = new HashMap<String, String>();
		response.initMap(sourceFile, map);
		assertEquals("text/html;charset=gb2312",map.get(".html"));
		assertEquals("text/html;charset=gb2312",map.get(".htm"));
		assertEquals("text/xml",map.get(".xml"));
		assertEquals("text/plain",map.get(".txt"));
		assertEquals("text/plain",map.get(".txt"));
		assertEquals("text/plain",map.get(".txt"));
		assertEquals("text/plain",map.get(".txt"));
		assertEquals("text/plain",map.get(".c"));
		assertEquals("text/plain",map.get(".cpp"));
		assertEquals("text/plain",map.get(".java"));
		assertEquals("text/plain",map.get(".pl"));
		assertEquals("text/plain",map.get(".cc"));
		assertEquals("text/plain",map.get(".h"));
		assertEquals("text/css",map.get(".css"));
		assertEquals("image/gif",map.get(".gif"));
		assertEquals("image/png",map.get(".png"));
		assertEquals("image/jpeg",map.get(".jpeg"));
		assertEquals("image/jpeg",map.get(".jpg"));
		
		assertNull(map.get(""));
		assertNull(map.get(null));
		assertNull(map.get("xienigewnifwjigw"));
		assertNull(map.get(".abc"));
		assertNull(map.get(".TXt"));
	}
}
