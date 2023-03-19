package hu.lanoga.toolbox.util;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public class MarkdownHtmlConverterUtil {


	public static String convertMarkdownToHtmlString(String markdownStr) {

		final Parser parser = Parser.builder().build();
		
		markdownStr = markdownStr.replaceAll("\r", "").replaceAll("\n", "xxxnewlinexxx");
		markdownStr = Jsoup.clean(markdownStr, Safelist.none());
		markdownStr = markdownStr.replaceAll("xxxnewlinexxx", "\n");
		final Node document = parser.parse(markdownStr);

		final HtmlRenderer renderer = HtmlRenderer.builder().build();
		return renderer.render(document);
		
	}

}
