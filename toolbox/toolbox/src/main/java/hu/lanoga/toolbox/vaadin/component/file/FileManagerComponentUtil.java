package hu.lanoga.toolbox.vaadin.component.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.vaadin.server.FontAwesome;
import com.vaadin.server.FontIcon;

import hu.lanoga.toolbox.ToolboxSysKeys.FileOperationAccessTypeIntent;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileStoreHelper;

/**
 * a FileManagerComponent használja, illetve annak kapcsán pár helper metódus
 * 
 * @see FileManagerComponentBuilder
 * @see FileManagerComponent
 * @see FileUploadComponent
 */
public final class FileManagerComponentUtil {

	private FileManagerComponentUtil() {
		//
	}

	/**
	 * @param fileIdsList 
	 * 		egy vagy több String, ahol egy-egy String egy-egy JsonArray (ahogyan a DB is tárolni szoktuk) 
	 * 		(értsd: több model objektum fájljait együtt is meg lehet jeleníteni)
	 * @return
	 * 
	 * @deprecated
	 * @see FileStoreHelper#toFileDescriptorList(String...)
	 */
	@Deprecated
	public static List<FileDescriptor> toFileDescriptorList(final String... fileIdsList) {
		return FileStoreHelper.toFileDescriptorList(FileOperationAccessTypeIntent.CHANGE_INTENT, fileIdsList);
	}

	static File getLastModifiedFile(final File directory) {
		final File[] files = directory.listFiles();
		if (files.length == 0) {
			return null;
		}
		Arrays.sort(files, new Comparator<File>() {

			@Override
			public int compare(final File o1, final File o2) {
				return new Long(o2.lastModified()).compareTo(o1.lastModified());
			}
		});
		return files[0];
	}

	static List<Path> listFiles(final String prefix, final File dir, final Locale comparatorLocale) throws IOException {

		final List<Path> list = Files.list(dir.toPath()).filter(f -> StringUtils.startsWithIgnoreCase(f.getFileName().toString(), prefix)).collect(Collectors.toList());

		if (comparatorLocale != null) {

			final Collator collator = Collator.getInstance(comparatorLocale);

			list.sort(new Comparator<Path>() {

				@Override
				public int compare(final Path o1, final Path o2) {
					return collator.compare(o1.getFileName().toString(), o2.getFileName().toString());
				}
			});

		}

		return list;
	}

	@SuppressWarnings("unused")
	static FontIcon getFileIcon(final String filename, final long fileSize) {

		final String ext = FilenameUtils.getExtension(filename).toLowerCase();

		switch (ext) {

		case "pdf":
			return FontAwesome.FILE_PDF_O;
		case "doc":
			return FontAwesome.FILE_WORD_O;
		case "docx":
			return FontAwesome.FILE_WORD_O;
		case "xls":
			return FontAwesome.FILE_EXCEL_O;
		case "xlsx":
			return FontAwesome.FILE_EXCEL_O;
		case "ppt":
			return FontAwesome.FILE_POWERPOINT_O;
		case "pptx":
			return FontAwesome.FILE_POWERPOINT_O;
		case "txt":
			return FontAwesome.FILE_TEXT_O;
		case "rtf":
			return FontAwesome.FILE_TEXT_O;
		case "csv":
			return FontAwesome.FILE_TEXT_O;
		case "html":
			return FontAwesome.FILE_CODE_O;
		case "xml":
			return FontAwesome.FILE_CODE_O;
		case "java":
			return FontAwesome.FILE_CODE_O;
		case "sql":
			return FontAwesome.FILE_CODE_O;
		case "js":
			return FontAwesome.FILE_CODE_O;
		case "css":
			return FontAwesome.FILE_CODE_O;
		case "png":
			return FontAwesome.FILE_PICTURE_O;
		case "jpeg":
			return FontAwesome.FILE_PICTURE_O;
		case "jpg":
			return FontAwesome.FILE_PICTURE_O;
		case "gif":
			return FontAwesome.FILE_PICTURE_O;
		case "bmp":
			return FontAwesome.FILE_PICTURE_O;
		case "mp3":
			return FontAwesome.FILE_AUDIO_O;
		case "wav":
			return FontAwesome.FILE_AUDIO_O;
		case "mp4":
			return FontAwesome.FILE_VIDEO_O;
		case "avi":
			return FontAwesome.FILE_VIDEO_O;
		case "mkv":
			return FontAwesome.FILE_VIDEO_O;
		case "7z":
			return FontAwesome.FILE_ARCHIVE_O;
		case "zip":
			return FontAwesome.FILE_ARCHIVE_O;
		case "rar":
			return FontAwesome.FILE_ARCHIVE_O;

		default:
			return FontAwesome.FILE_O;
		}
	}

}
