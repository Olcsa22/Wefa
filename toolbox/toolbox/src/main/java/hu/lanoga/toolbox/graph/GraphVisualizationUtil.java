//package hu.lanoga.toolbox.graph;
//
//import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
//import com.mxgraph.layout.mxIGraphLayout;
//import com.mxgraph.util.mxCellRenderer;
//import com.mxgraph.util.mxConstants;
//import com.mxgraph.view.mxStylesheet;
//import hu.lanoga.toolbox.ToolboxSysKeys;
//import hu.lanoga.toolbox.file.FileDescriptor;
//import hu.lanoga.toolbox.file.FileStoreService;
//import hu.lanoga.toolbox.spring.ApplicationContextHelper;
//import hu.lanoga.toolbox.spring.SecurityUtil;
//import org.apache.commons.codec.digest.DigestUtils;
//import org.apache.commons.lang3.tuple.Triple;
//import org.jgrapht.ext.JGraphXAdapter;
//import org.jgrapht.graph.DefaultDirectedGraph;
//import org.jgrapht.graph.DefaultEdge;
//
//import javax.imageio.ImageIO;
//import java.awt.*;
//import java.awt.image.BufferedImage;
//import java.io.File;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.LinkedHashSet;
//import java.util.Map;
//
//public class GraphVisualizationUtil {
//
//	/**
//	 *
//	 * @param points
//	 * @param edges
//	 * @param orientation
//	 * 			az orientation paraméter-nél SwingConstants-ot várunk be (pl: SwingConstants.WEST, ebben az esetben balról jobbra fog megjelenítődni)
//	 * @return
//	 * @throws IOException
//	 */
//	public static FileDescriptor generateDirectedGraph(final LinkedHashSet<String> points, final LinkedHashSet<Triple<String, String, String>> edges, final Integer orientation) throws IOException {
//
//		final FileStoreService fileStoreService = ApplicationContextHelper.getBean(FileStoreService.class);
//
//		final DefaultDirectedGraph<String, DefaultEdge> directedGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
//
//		for (String point : points) {
//			directedGraph.addVertex(point);
//		}
//
//		for (Triple<String, String, String> edge : edges) {
//			directedGraph.addEdge(edge.getLeft(), edge.getMiddle(), new RelationshipEdge(edge.getRight()));
//		}
//
//		final JGraphXAdapter<String, DefaultEdge> graphAdapter = new JGraphXAdapter<>(directedGraph);
//
//		final mxIGraphLayout layout = new mxHierarchicalLayout(graphAdapter);
//		if (orientation != null) {
//			((mxHierarchicalLayout) layout).setOrientation(orientation);
//		}
//
//		final Map<String, Object> edgeStyle = new HashMap<>();
//
//		edgeStyle.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_CONNECTOR);
//		edgeStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_CLASSIC);
//		edgeStyle.put(mxConstants.STYLE_STROKECOLOR, "#000000");
//		edgeStyle.put(mxConstants.STYLE_FONTCOLOR, "#000000");
//		edgeStyle.put(mxConstants.STYLE_LABEL_BACKGROUNDCOLOR, "#ffffff");
//
//		final mxStylesheet stylesheet = new mxStylesheet();
//		stylesheet.setDefaultEdgeStyle(edgeStyle);
//		graphAdapter.setStylesheet(stylesheet);
//
//		layout.execute(graphAdapter.getDefaultParent());
//
//		final String listsToString = SecurityUtil.getLoggedInUserTenantId() + points.toString() + edges.toString();
//		final String encodedFilename = DigestUtils.md5Hex(listsToString);
//
//		final FileDescriptor fd = fileStoreService.createTmpFile2(encodedFilename + ".png", ToolboxSysKeys.FileDescriptorLocationType.PUBLIC_FOLDER_FOR_CDN, ToolboxSysKeys.FileDescriptorSecurityType.AUTHENTICATED_USER);
//
//		final BufferedImage image = mxCellRenderer.createBufferedImage(graphAdapter, null, 2, Color.WHITE, true, null);
//		final File imgFile = new File(fd.getFile().getAbsolutePath());
//		ImageIO.write(image, "PNG", imgFile);
//
//		imgFile.createNewFile();
//
//		return fd;
//	}
//
//}
