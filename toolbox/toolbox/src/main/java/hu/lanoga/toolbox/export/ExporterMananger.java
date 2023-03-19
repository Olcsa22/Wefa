package hu.lanoga.toolbox.export;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Component;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.controller.DefaultCrudController;
import hu.lanoga.toolbox.exception.ToolboxInterruptedException;
import hu.lanoga.toolbox.export.pdf.advanced.AdvancedPdfExporterUtil;
import hu.lanoga.toolbox.export.pdf.simple.SimplePdfExporterUtil;
import hu.lanoga.toolbox.export.xlsx.XlsxExporterUtil;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.util.ToolboxAssert;
import hu.lanoga.toolbox.util.ToolboxStringUtil;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridComponent;
import lombok.extern.slf4j.Slf4j;

/**
 * @see DefaultCrudController#findAllPaged(String, String)
 * @see CrudGridComponent#getBtnExport()
 */
@Slf4j
@ConditionalOnMissingBean(name = "exporterManangerOverrideBean")
@Component
public class ExporterMananger {

	@Autowired
	private FileStoreService fileStoreService;

	@Secured(ToolboxSysKeys.UserAuth.ROLE_USER_STR) // ez csak az export részre vonatkozik, ha a service metódus szigorúbb, akkor ez nem gyengíti
	public <T> Triple<FileDescriptor, String, String> export(final List<T> list, final Class<T> modelClass, final Integer exportType) throws ExporterException {

		log.debug("ExportManager, exportType: " + exportType);

		return exportInner(list, modelClass, exportType);
	}

	private <T> Triple<FileDescriptor, String, String> exportInner(final List<T> list, final Class<T> modelClass, final Integer exportType) throws ExporterException {
		
		FileDescriptor tmpFile = null;

		try {

			String filename = "export_" + ToolboxStringUtil.camelCaseToUnderscore(modelClass.getSimpleName()) + "_" + new SimpleDateFormat("yyyy_MM_dd").format(new Date());
			String mimeType = "";

			if (ToolboxSysKeys.ExportType.XLSX == exportType) {

				filename += ".xlsx";
				mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

				tmpFile = fileStoreService.createTmpFile2(filename, ToolboxSysKeys.FileDescriptorLocationType.PROTECTED_FOLDER, ToolboxSysKeys.FileDescriptorSecurityType.ADMIN_OR_CREATOR);

				xlsxInner(modelClass, list, tmpFile);

			} else if ((ToolboxSysKeys.ExportType.PDF == exportType)) {

				mimeType = "application/pdf";

				final ExporterOptions exporterAnnotation = modelClass.getAnnotation(ExporterOptions.class);

				if (exporterAnnotation != null) {

					ToolboxAssert.isTrue(StringUtils.isNotBlank(exporterAnnotation.advancedPdfTemplateFile()));

					filename += ".pdf";
					tmpFile = fileStoreService.createTmpFile2(filename, ToolboxSysKeys.FileDescriptorLocationType.PROTECTED_FOLDER, ToolboxSysKeys.FileDescriptorSecurityType.ADMIN_OR_CREATOR);

					pdfInnerAdvanced(modelClass, list, tmpFile, exporterAnnotation);

				} else {

					filename += ".pdf";
					tmpFile = fileStoreService.createTmpFile2(filename, ToolboxSysKeys.FileDescriptorLocationType.PROTECTED_FOLDER, ToolboxSysKeys.FileDescriptorSecurityType.ADMIN_OR_CREATOR);

					pdfInnerSimple(modelClass, list, tmpFile);

				}

			} else {

				throw new IllegalArgumentException("No matching exporter found!");

			}

			return Triple.of(tmpFile, filename, mimeType);

		} catch (final Exception e) {

			try {

				if (tmpFile != null) {
					fileStoreService.setToBeDeleted(tmpFile.getId());
				}

			} catch (final Exception e2) {
				//
			}

			if (e instanceof ToolboxInterruptedException) {
				throw e;
			} else {
				throw new ExporterException(e);
			}
			
		}
		
	}

	protected <T> void pdfInnerSimple(final Class<T> modelClass, final List<T> list, FileDescriptor tmpFile) {
		SimplePdfExporterUtil.generateReport(tmpFile.getFile(), list, modelClass, null);
	}

	protected <T> void pdfInnerAdvanced(final Class<T> modelClass, final List<T> list, FileDescriptor tmpFile, final ExporterOptions exporterAnnotation) {
		AdvancedPdfExporterUtil.generateReport(tmpFile.getFile(), exporterAnnotation.advancedPdfTemplateFile(), list, modelClass);
	}

	protected <T> void xlsxInner(final Class<T> modelClass, final List<T> list, FileDescriptor tmpFile) {
		XlsxExporterUtil.generateXlsxViaReflection(tmpFile.getFile(), list, modelClass, null);
	}

}
