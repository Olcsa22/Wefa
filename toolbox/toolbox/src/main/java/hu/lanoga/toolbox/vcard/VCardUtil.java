package hu.lanoga.toolbox.vcard;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.VCardVersion;
import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.FileDescriptorLocationType;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;

public final class VCardUtil {

	private VCardUtil() {
		//
	}

	private static ToolboxVCardDataToEzVCardConverter toolboxVCardDataToEzVCardConverter = new ToolboxVCardDataToEzVCardConverter();
	
	/**
	 * letöltéshez... (kiírja/kigenerálja a reponse {@code OutputStream}-jébe)
	 * 
	 * @param vCardObject
	 * @param response
	 */
	public static void writeVCard(Object vCardObject, HttpServletResponse response) {

		FileDescriptor tmpFile = null;

			try {
				if (vCardObject instanceof ezvcard.VCard) {

					VCard vCard = (VCard) vCardObject;

					tmpFile = ApplicationContextHelper.getBean(FileStoreService.class).createTmpFile2("vcard.vcf", FileDescriptorLocationType.PROTECTED_FOLDER, ToolboxSysKeys.FileDescriptorSecurityType.ADMIN_OR_CREATOR);

					Ezvcard.write(vCard).version(VCardVersion.V3_0).go(tmpFile.getFile());

					final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

					response.setHeader("Content-Type", "text/vcard; charset=ISO-8859-1");
					response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(vCard.getFormattedName().getValue() + " - " + simpleDateFormat.format(new Date()) + ".vcf", "UTF-8").replace("+", "%20"));

					try (InputStream inputStream = new BufferedInputStream(new FileInputStream(tmpFile.getFile()), 32 * 1024); OutputStream outputStream = response.getOutputStream()) {
						IOUtils.copyLarge(inputStream, outputStream);
					}
				} else {
					throw new VCardException("Instance of vCardObject is not supported.");
				}
			} catch (Exception e) {
				throw new VCardException("VCard write failed!", e);
			} finally {
				if (tmpFile != null) {
					ApplicationContextHelper.getBean(FileStoreService.class).setToBeDeleted(tmpFile.getId());
				}

			}
	}

	/**
	 * @param vCardData
	 *            név (VNÉV, KNÉV) sorrendet szabályozza, null esetén default (angol sorrend)
	 * @return
	 */
	public static Object buildVCard(ToolboxVCardData vCardData) {
		return toolboxVCardDataToEzVCardConverter.convert(vCardData);
	}

}
