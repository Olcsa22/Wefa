//package hu.lanoga.toolbox.vaadin.component;
//
//import java.io.IOException;
//import java.util.LinkedHashSet;
//
//import javax.swing.SwingConstants;
//
//import org.apache.commons.codec.digest.DigestUtils;
//import org.apache.commons.lang3.tuple.Triple;
//
//import com.vaadin.shared.ui.ContentMode;
//import com.vaadin.ui.Alignment;
//import com.vaadin.ui.Label;
//import com.vaadin.ui.VerticalLayout;
//
//import hu.lanoga.toolbox.exception.ToolboxGeneralException;
//import hu.lanoga.toolbox.file.FileDescriptor;
//import hu.lanoga.toolbox.file.FileStoreHelper;
//import hu.lanoga.toolbox.file.FileStoreService;
//import hu.lanoga.toolbox.graph.GraphVisualizationUtil;
//import hu.lanoga.toolbox.spring.ApplicationContextHelper;
//import hu.lanoga.toolbox.spring.SecurityUtil;
//
//public class GraphVisualizationComponent extends VerticalLayout {
//
//	private final LinkedHashSet<String> points;
//	private final LinkedHashSet<Triple<String, String, String>> edges;
//
//	public GraphVisualizationComponent(final LinkedHashSet<String> points, final LinkedHashSet<Triple<String, String, String>> edges) {
//		
//		this.points = points;
//		this.edges = edges;
//
//		this.setDefaultComponentAlignment(Alignment.MIDDLE_CENTER);
//		final FileStoreService fileStoreService = ApplicationContextHelper.getBean(FileStoreService.class);
//
//		final Label lblImg = new Label();
//		lblImg.setWidth(100, Unit.PERCENTAGE);
//		lblImg.setContentMode(ContentMode.HTML);
//	
//		// ---
//		
//		FileDescriptor fileDescriptor;
//
//		// ---
//		
//		final String listsToString = SecurityUtil.getLoggedInUserTenantId() + points.toString() + edges.toString();
//		final String encodedFilename = DigestUtils.md5Hex(listsToString) + ".png";
//
//		final FileDescriptor fileWithMatchingFilename = fileStoreService.getFileWithMatchingFilename(encodedFilename);
//
//		if (fileWithMatchingFilename != null) {
//			fileDescriptor = fileWithMatchingFilename;
//		} else {
//			try {
//				fileDescriptor = GraphVisualizationUtil.generateDirectedGraph(points, edges, SwingConstants.WEST);
//				fileStoreService.setToNormal(fileDescriptor.getId());
//			} catch (final IOException e) {
//				throw new ToolboxGeneralException("GraphVisualizationComponent error!", e);
//			}
//
//		}
//
//		// ---
//
//		final String path = FileStoreHelper.generateDownloadUrlPublicCdnFolder(fileDescriptor.getId(), false);
//
//		lblImg.setValue("<img src=" + path + ">");
//
//		this.addComponent(lblImg);
//	}
//
//	public void initLayout() {
//		this.removeAllComponents();
//
//		this.setDefaultComponentAlignment(Alignment.MIDDLE_CENTER);
//		final FileStoreService fileStoreService = ApplicationContextHelper.getBean(FileStoreService.class);
//
//		final Label lblImg = new Label();
//		lblImg.setWidth(100, Unit.PERCENTAGE);
//		lblImg.setContentMode(ContentMode.HTML);
//		FileDescriptor fileDescriptor;
//
//		// ---
//		final String listsToString = SecurityUtil.getLoggedInUserTenantId() + this.points.toString() + this.edges.toString();
//		final String encodedFilename = DigestUtils.md5Hex(listsToString) + ".png";
//
//		final FileDescriptor fileWithMatchingFilename = fileStoreService.getFileWithMatchingFilename(encodedFilename);
//
//		if (fileWithMatchingFilename != null) {
//			fileDescriptor = fileWithMatchingFilename;
//		} else {
//			try {
//				fileDescriptor = GraphVisualizationUtil.generateDirectedGraph(this.points, this.edges, SwingConstants.WEST);
//				fileStoreService.setToNormal(fileDescriptor.getId());
//			} catch (final IOException e) {
//				throw new ToolboxGeneralException("GraphVisualizationComponent error!", e);
//			}
//
//		}
//
//		// ---
//
//		final String path = FileStoreHelper.generateDownloadUrlPublicCdnFolder(fileDescriptor.getId(), false);
//
//		lblImg.setValue("<img src=" + path + ">");
//
//		this.addComponent(lblImg);
//	}
//}
