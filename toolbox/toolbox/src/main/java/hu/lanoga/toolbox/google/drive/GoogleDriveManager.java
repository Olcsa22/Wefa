//package hu.lanoga.toolbox.google.drive;
//
//import java.io.BufferedOutputStream;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.io.OutputStream;
//import java.nio.file.Files;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Date;
//import java.util.List;
//import java.util.TimeZone;
//
//import javax.annotation.PostConstruct;
//
//import org.apache.commons.lang3.StringUtils;
//import org.apache.commons.lang3.tuple.Pair;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
//import org.springframework.core.io.Resource;
//import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
//import org.springframework.stereotype.Component;
//
//import com.google.api.client.auth.oauth2.Credential;
//import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
//import com.google.api.client.googleapis.batch.BatchRequest;
//import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
//import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
//import com.google.api.client.googleapis.json.GoogleJsonError;
//import com.google.api.client.googleapis.media.MediaHttpUploader;
//import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
//import com.google.api.client.http.FileContent;
//import com.google.api.client.http.HttpHeaders;
//import com.google.api.client.json.jackson2.JacksonFactory;
//import com.google.api.services.drive.Drive;
//import com.google.api.services.drive.Drive.Files.Create;
//import com.google.api.services.drive.model.File;
//import com.google.api.services.drive.model.Permission;
//import com.google.api.services.sheets.v4.Sheets;
//import com.google.api.services.sheets.v4.model.ValueRange;
//import com.google.common.collect.Lists;
//
//import hu.lanoga.toolbox.exception.ToolboxGeneralException;
//import hu.lanoga.toolbox.file.FileDescriptor;
//import hu.lanoga.toolbox.quickcontact.QuickContact;
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//@Component
//@ConditionalOnMissingBean(name = "googleDriveManagerOverrideBean")
//public class GoogleDriveManager {
//
//	// TODO: ez nem TenantKeyValueSettings kompatiblis (tehát csak egy fix globális Google fiókot tud használni)
//	
//	@Value("${tools.google.drive.service-account.json}")
//	private String serviceAccountJson;
//
//	@Value("${tools.misc.application-name-lower-case}")
//	private String appNameLc;
//
//	@Value("#{'${tools.google.drive.auto-share.with}'.split(',')}")
//	private List<String> shareWith;
//
//	private Drive drive;
//
//	private Sheets sheets;
//
//	private GoogleDriveManager() {
//		//
//	}
//
//	@PostConstruct
//	private void init() {
//
//		try {
//
//			final Resource r = new PathMatchingResourcePatternResolver().getResources(serviceAccountJson)[0];
//
//			if (!r.exists()) {
//				GoogleDriveManager.log.debug("init skipped (missing service account JSON file)");
//				return;
//			}
//
//			final Credential credential = GoogleCredential
//					.fromStream(r.getInputStream())
//					.createScoped(Lists.newArrayList("https://www.googleapis.com/auth/drive"));
//
//			this.drive = new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), credential).setApplicationName(this.appNameLc).build();
//			this.sheets = new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), credential).setApplicationName(this.appNameLc).build();
//
//		} catch (final Exception e) {
//
//			final GoogleDriveManagerException ex = new GoogleDriveManagerException("init failed! missing service account JSON file: " + serviceAccountJson, e);
//			GoogleDriveManager.log.error("init failed", ex);
//			throw ex;
//
//		}
//
//	}
//
//	/**
//	 * @param folderName a könyvtárra mutató Google-féle File objektum
//	 * @return
//	 */
//	private com.google.api.services.drive.model.File createFolderIfNotExists(final String folderName) {
//
//		try {
//
//			final String qStr = "mimeType='application/vnd.google-apps.folder' and name = '" + folderName + "'";
//
//			List<com.google.api.services.drive.model.File> items = this.drive.files().list().setQ(qStr).execute().getFiles();
//
//			if (items.isEmpty()) {
//
//				{
//					final com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
//					fileMetadata.setName(folderName);
//					fileMetadata.setMimeType("application/vnd.google-apps.folder");
//
//					GoogleDriveManager.log.debug("createFolderIfNotExists, create: " + folderName);
//
//					this.drive.files().create(fileMetadata).setFields("id").execute();
//				}
//
//				items = this.drive.files().list().setQ(qStr).execute().getFiles();
//
//				this.shareWith(items.get(0).getId(), false, shareWith);
//			}
//
//			return items.get(0); // ez a könyvtárra mutató Google-féle File objektum
//
//		} catch (final Exception e) {
//			throw new GoogleDriveManagerException("createFolder failed!", e);
//		}
//
//	}
//
//	private void shareWith(final String googleId, final boolean allowWrite, final List<String> emailAddresses) {
//
//		try {
//
//			final BatchRequest batchRequest = this.drive.batch();
//
//			// ---
//
//			final JsonBatchCallback<Void> callback1 = new JsonBatchCallback<Void>() {
//
//				@Override
//				public void onFailure(final GoogleJsonError googleJsonError, final HttpHeaders responseHeaders) throws IOException {
//					GoogleDriveManager.log.debug("permission remove failed: " + googleJsonError);
//				}
//
//				@Override
//				public void onSuccess(final Void v, final HttpHeaders responseHeaders) throws IOException {
//					GoogleDriveManager.log.debug("permission remove successful");
//				}
//			};
//
//			final List<Permission> currentList = this.drive.permissions().list(googleId).execute().getPermissions();
//
//			for (final Permission p : currentList) {
//				this.drive.permissions().delete(googleId, p.getId()).queue(batchRequest, callback1);
//			}
//
//			// ---
//
//			final JsonBatchCallback<Permission> callback2 = new JsonBatchCallback<Permission>() {
//
//				@Override
//				public void onFailure(final GoogleJsonError googleJsonError, final HttpHeaders responseHeaders) {
//					GoogleDriveManager.log.debug("permission grant failed: " + googleJsonError);
//				}
//
//				@Override
//				public void onSuccess(final Permission permission, final HttpHeaders responseHeaders) {
//					GoogleDriveManager.log.debug("permission grant successful: " + permission);
//				}
//			};
//
//			for (final String emailAddress : emailAddresses) {
//
//				final Permission userPermission = new Permission()
//						.setType("user")
//						.setRole(allowWrite ? "writer" : "reader")
//						.setEmailAddress(emailAddress);
//
//				this.drive.permissions().create(googleId, userPermission)
//						.setFields("id")
//						.queue(batchRequest, callback2);
//
//			}
//
//			// ---
//
//			batchRequest.execute();
//
//		} catch (final Exception e) {
//			throw new GoogleDriveManagerException("shareWith failed!", e);
//		}
//
//	}
//
//	public void shareWithUrl(final String googleId) {
//
//		try {
//
//			final BatchRequest batchRequest = this.drive.batch();
//
//			// ---
//
//			final JsonBatchCallback<Permission> callback = new JsonBatchCallback<Permission>() {
//
//				@Override
//				public void onFailure(final GoogleJsonError googleJsonError, final HttpHeaders responseHeaders) {
//					GoogleDriveManager.log.debug("permission grant failed: " + googleJsonError);
//				}
//
//				@Override
//				public void onSuccess(final Permission permission, final HttpHeaders responseHeaders) {
//					GoogleDriveManager.log.debug("permission grant successful: " + permission);
//				}
//			};
//
//			final Permission userPermission = new Permission()
//					.setType("anyone")
//					.setRole("reader")
//					.setAllowFileDiscovery(false);
//
//			this.drive.permissions().create(googleId, userPermission)
//					.setFields("id")
//					.queue(batchRequest, callback);
//
//			// ---
//
//			batchRequest.execute();
//
//		} catch (final Exception e) {
//			throw new GoogleDriveManagerException("shareWith failed!", e);
//		}
//
//	}
//
//	/**
//	 * @param file
//	 * 		ha null, akkor nincs feltöltés (csak könyvtár létrehozás stb.)
//	 * @param folderName
//	 * 		Google Drive könyvtárnév, létrehozza, ha nem létezik még {@link #createFolderIfNotExists(String)}
//	 * @param filename
//	 * 		ha nem null/üres/blank, akkor ez lesz a feltöltött fájl neve (null esetén az eredeti fájlnév)
//	 * @param mimeType
//	 * 		null esetén: {@link Files#probeContentType(java.nio.file.Path)}
//	 * 
//	 * @return google id + web view url
//	 */
//	public Pair<String, String> uploadFile(final java.io.File file, final String folderName, final String filename, final String mimeType) {
//
//		try {
//
//			// ---------- könyvtár keresése/létrehozása
//
//			final com.google.api.services.drive.model.File folderFileMetadata = this.createFolderIfNotExists(folderName);
//
//			// ---------- fájl feltöltése
//
//			final String filename2;
//
//			if (StringUtils.isNotBlank(filename)) {
//				filename2 = filename;
//			} else {
//				filename2 = file.getName();
//			}
//
//			com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
//			fileMetadata.setName(filename2);
//
//			fileMetadata.setParents(Collections.singletonList(folderFileMetadata.getId()));
//
//			String contentType = "application/octet-stream";
//
//			if (StringUtils.isNotBlank(mimeType)) {
//				contentType = mimeType;
//			} else {
//
//				try {
//					contentType = Files.probeContentType(file.toPath());
//				} catch (final Exception e) {
//					GoogleDriveManager.log.warn("uploadFile probeContentType failed!", e);
//				}
//
//			}
//
//			final FileContent mediaContent = new FileContent(contentType, file);
//
//			final Create create = this.drive.files().create(fileMetadata, mediaContent).setFields("id, webViewLink");
//
//			final MediaHttpUploader uploader = create.getMediaHttpUploader();
//			uploader.setDirectUploadEnabled(file.length() < (5L * 1024L * 1024L));
//
//			uploader.setProgressListener(new MediaHttpUploaderProgressListener() {
//
//				@Override
//				public void progressChanged(final MediaHttpUploader mediaHttpUploader) throws IOException {
//
//					GoogleDriveManager.log.debug("uploadFile process: " + filename2 + ", " + mediaHttpUploader.getUploadState().name());
//
//				}
//			});
//
//			fileMetadata = create.execute();
//
//			// ---
//
//			return Pair.of(fileMetadata.getId(), fileMetadata.getWebViewLink());
//
//		} catch (final Exception e) {
//
//			throw new GoogleDriveManagerException("uploadFile failed!", e);
//
//		}
//
//	}
//
//	public void updateQuickContactFile(final String folderName, final String filename, final QuickContact quickContact) {
//
//		try {
//
//			final String spreadsheetId = getSpreadsheet(folderName, filename);
//
//			String range = "A1:X";
//
//			// ---------- lekérjük a táblázatból az adatokat és meghatározzuk az első üres sort
//
//			final ValueRange response = this.sheets.spreadsheets().values().get(spreadsheetId, range).execute();
//
//			final List<List<Object>> values = response.getValues();
//			final int firstEmptyRow;
//
//			if (values != null) {
//				firstEmptyRow = values.size() + 1;
//			} else {
//				firstEmptyRow = 1;
//			}
//
//			// ---------- adatok összeállítása
//
//			final List<Object> valuesRow = new ArrayList<>();
//
//			if (quickContact.getOrigin() != null) {
//				valuesRow.add(quickContact.getOrigin());
//			} else {
//				valuesRow.add("");
//			}
//
//			final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//			sdf.setTimeZone(TimeZone.getTimeZone("CET"));
//			final String utcTime = sdf.format(new Date());
//
//			valuesRow.add(utcTime);
//
//			if (quickContact.getContactName() != null) {
//				valuesRow.add(quickContact.getContactName());
//			} else {
//				valuesRow.add("");
//			}
//
//			if (quickContact.getPhoneNumber() != null) {
//				valuesRow.add(quickContact.getPhoneNumber());
//			} else {
//				valuesRow.add("");
//			}
//
//			if (quickContact.getEmail() != null) {
//				valuesRow.add(quickContact.getEmail());
//			} else {
//				valuesRow.add("");
//			}
//
//			if (quickContact.getCompanyName() != null && StringUtils.isNotBlank(quickContact.getCompanyName())) {
//				valuesRow.add("cég");
//				valuesRow.add(quickContact.getCompanyName());
//			} else {
//				valuesRow.add("");
//				valuesRow.add("");
//			}
//
//			if (quickContact.getCity() != null) {
//				valuesRow.add(quickContact.getCity());
//			} else {
//				valuesRow.add("");
//			}
//
//			if (quickContact.getCityDetails() != null) {
//				valuesRow.add(quickContact.getCityDetails());
//			} else {
//				valuesRow.add("");
//			}
//
//			if (quickContact.getNote() != null) {
//				valuesRow.add(quickContact.getNote());
//			} else {
//				valuesRow.add("");
//			}
//
//			// ---------- mentés spreadsheet-be
//
//			final List<List<Object>> valuesToSave = new ArrayList<>();
//			valuesToSave.add(valuesRow);
//
//			final ValueRange body = new ValueRange().setValues(valuesToSave);
//
//			range = "A" + firstEmptyRow;
//
//			// "Updates require a valid ValueInputOption parameter (for singular updates, this is a required query parameter; for batch updates, this parameter is required in the request body).
//			// The ValueInputOption controls whether input strings are parsed or not."
//			//
//			// RAW - The input is not parsed and is simply inserted as a string.
//			// USER_ENTERED - The input is parsed exactly as if it were entered into the Google Sheets UI, so "Mar 1 2016" becomes a date, and "=1+2" becomes a formula.
//			//
//			// https://developers.google.com/sheets/api/guides/values
//
//			final String valueInputOption = "USER_ENTERED";
//
//			sheets.spreadsheets().values()
//					.append(spreadsheetId, range, body)
//					.setValueInputOption(valueInputOption)
//					.execute();
//
//		} catch (final Exception e) {
//			throw new ToolboxGeneralException("updateQuickContactFile failed!", e);
//		}
//
//	}
//
//	/**
//	 * @param folderName
//	 * 		ha null értékkel adjuk be, akkor a root könyvtárban fog keresni és nem mappában
//	 * @param filename
//	 * @return fileId
//	 * 		(a google drive által kigenerált fileId-ja, ami alapján be lehet azonosítani)
//	 * @throws IOException
//	 */
//	private String getSpreadsheet(final String folderName, final String filename) throws IOException {
//
//		File folder = null;
//		List<File> files;
//
//		if (StringUtils.isNotBlank(folderName)) {
//			folder = createFolderIfNotExists(folderName);
//			files = drive.files().list().setQ("mimeType='application/vnd.google-apps.spreadsheet' and name = '" + filename + "' and '" + folder.getId() + "' in parents").execute().getFiles();
//		} else {
//			files = drive.files().list().setQ("mimeType='application/vnd.google-apps.spreadsheet' and name = '" + filename + "'").execute().getFiles();
//		}
//
//		if (!files.isEmpty()) {
//			final File file = files.get(0);
//			return file.getId();
//		} else {
//
//			// ez létrehozta a spreadsheetet, ha nem létezett
//			// ideiglenesen kikapcsolva
//
//			// Spreadsheet spreadsheet = new Spreadsheet()
//			// .setProperties(new SpreadsheetProperties()
//			// .setTitle(filename));
//			//
//			// spreadsheet = this.sheets.spreadsheets().create(spreadsheet)
//			// .setFields("spreadsheetId")
//			// .execute();
//			//
//			// // ha egy adott mappában kell létrehozni a fájlt
//			// if (folderName != null && folder != null) {
//			//
//			// final File file = drive.files().get(spreadsheet.getSpreadsheetId()).setFields("parents").execute();
//			//
//			// final StringBuilder previousParents = new StringBuilder();
//			//
//			// for (String parent : file.getParents()) {
//			// previousParents.append(parent);
//			// previousParents.append(',');
//			// }
//			//
//			// final File updatedFile = drive.files().update(spreadsheet.getSpreadsheetId(), null)
//			// .setRemoveParents(previousParents.toString())
//			// .setAddParents(folder.getId())
//			// .setFields("id, parents")
//			// .execute();
//			//
//			// return updatedFile.getId();
//			// }
//
//			throw new ToolboxGeneralException("No file with the name: " + filename + " exists!");
//		}
//
//	}
//
//	public void givePermission(String fileId, String email) throws IOException {
//
//		JsonBatchCallback<Permission> callback = new JsonBatchCallback<Permission>() {
//
//			@Override
//			public void onFailure(GoogleJsonError e, HttpHeaders responseHeaders) throws IOException {
//				throw new ToolboxGeneralException("Error during giving permission! " + e);
//			}
//
//			@Override
//			public void onSuccess(Permission permission, HttpHeaders responseHeaders) throws IOException {
//				log.info("Successful permission given. Permission ID: " + permission.getId());
//			}
//		};
//
//		BatchRequest batch = drive.batch();
//		Permission userPermission = new Permission()
//				.setType("user")
//				.setRole("writer")
//				.setEmailAddress(email);
//
//		drive.permissions().create(fileId, userPermission)
//				.setFields("id")
//				.queue(batch, callback);
//
//		batch.execute();
//	}
//
//	public FileDescriptor uploadExcelAndConvertToPdf(final FileDescriptor inputFile, final FileDescriptor outputFile) throws IOException {
//
//		final File fileMetadata = new File();
//		fileMetadata.setName(inputFile.getFilename());
//		fileMetadata.setMimeType("application/vnd.google-apps.spreadsheet");
//
//		final java.io.File filePath = inputFile.getFile();
//		final FileContent mediaContent = new FileContent("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", filePath);
//		final File file = drive.files().create(fileMetadata, mediaContent)
//				.setFields("id")
//				.execute();
//
//		try (OutputStream os = new BufferedOutputStream(new FileOutputStream(outputFile.getFile()), 64 * 1024)) {
//			drive.files().export(file.getId(), "application/pdf").executeMediaAndDownloadTo(os);
//		}
//
//		return outputFile;
//	}
//
//}
