package hu.lanoga.amazon.s3adv;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "s3advclient", version = "1.0", mixinStandardHelpOptions = true)
public class Start implements Runnable {

	private final static Logger log = Logger.getLogger(Start.class.getName());

	public static void main(final String[] args) {

		try {

			final int exitCode = new CommandLine(new Start()).execute(args);
			System.exit(exitCode);

		} catch (final Exception e) {
			log.log(Level.SEVERE, "main method error", e);
		}

	}

	@ArgGroup(exclusive = true, multiplicity = "0..1")
	DirectionOption directionOption = new DirectionOption(true, false); // default, ha semmi sincs megadva

	private static final class DirectionOption {

		public DirectionOption() {
			//
		}

		public DirectionOption(final boolean isDownload, final boolean isUpload) {
			this.isDownload = isDownload;
			this.isUpload = isUpload;
		}

		@Option(names = "-d", description = "download")
		boolean isDownload;
		@Option(names = "-u", description = "upload")
		boolean isUpload;
	}

	@Override
	public void run() {
		System.out.println(this.directionOption.isDownload);
		System.out.println(this.directionOption.isUpload);

		// ---

		{

			// MinioClient minioClient =
			// MinioClient.builder()
			// .endpoint("http://localhost:9000")
			// .credentials("test", "Abc12345")
			// .build();
			//
			// try {
			// minioClient.uploadObject(
			// UploadObjectArgs.builder()
			// .bucket("my-bucketname").object("my-objectname").filename("person.json").ra.build());
			// } catch (InvalidKeyException | ErrorResponseException | IllegalArgumentException | InsufficientDataException | InternalException | InvalidBucketNameException | InvalidResponseException | NoSuchAlgorithmException | ServerException | XmlParserException | IOException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }

		}

		// ---

		// {
		//
		// MinioClient minioClient = new MinioClient.builder()
		// .endpoint("http://localhost:9000")
		// .credentials("test", "Abc12345")
		// .build();
		//
		// minioClient.setBucketEncryption(SetBucketEncryptionArgs.builder().config(SseConfiguration.));
		//
		// }

		// ---

		final String endpointUrl = "http://localhost:9000";
		final String localRegionName = "-";

		final String accessKey = "bela";
		final String secretKey = "Abc12345";

		final String bucketName = "ketto";

		AmazonS3ClientBuilder amazonS3ClientBuilder = AmazonS3ClientBuilder.standard();

		if (endpointUrl != null && !endpointUrl.trim().isEmpty()) {
			final ClientConfiguration clientConfiguration = new ClientConfiguration();
			clientConfiguration.setSignerOverride("AWSS3V4SignerType");
			amazonS3ClientBuilder = amazonS3ClientBuilder.withClientConfiguration(clientConfiguration);
			amazonS3ClientBuilder = amazonS3ClientBuilder.withPathStyleAccessEnabled(Boolean.TRUE);
			amazonS3ClientBuilder = amazonS3ClientBuilder.withEndpointConfiguration(new EndpointConfiguration(endpointUrl, null));
		} else {
			amazonS3ClientBuilder = amazonS3ClientBuilder.withRegion(getRegion(localRegionName));
		}

		amazonS3ClientBuilder = amazonS3ClientBuilder.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)));
		final AmazonS3 amazonS3Client = amazonS3ClientBuilder.build();

		// ---

		{

			final String key = "Koala.jpg"; // filename bucket-en belül

			System.out.println("Downloading an object");

			final S3Object o1 = amazonS3Client.getObject(new GetObjectRequest(bucketName, key)/*.withRange(10001)*/);

			try (InputStream is = o1.getObjectContent();
					OutputStream os = new FileOutputStream(new File("D:/TMP/32/" + key), true)) {

				byte[] buf = new byte[8192];
				int length;
				while ((length = is.read(buf)) > 0) {
					os.write(buf, 0, length);
				}

			} catch (final IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		// ---

		{

			long startTs = System.currentTimeMillis();

			final String keyName = "city.png"; // filename bucket-en belül

			final File file = new File("d:/TMP/33/" + keyName);

			// Create a list of ETag objects. You retrieve ETags for each object part uploaded,
			// then, after each individual part has been uploaded, pass the list of ETags to
			// the request to complete the upload.
			List<PartETag> partETags = new ArrayList<PartETag>();

			Gson gson = new Gson();

			int alreadyUploadedCount = 0;

			final File fileJsonDescriptor = new File("d:/TMP/33/" + keyName + ".json");
			final File fileUpldId = new File("d:/TMP/33/" + keyName + ".uplid");

			if (fileJsonDescriptor.exists()) {

				try {

					PartETag[] x = gson.fromJson(new FileReader(fileJsonDescriptor), PartETag[].class);
					partETags.addAll(Arrays.asList(x));

					alreadyUploadedCount = partETags.size();

				} catch (JsonSyntaxException | JsonIOException | FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			String uploadId = null;

			if (partETags.isEmpty()) {

				InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(bucketName, keyName);
				InitiateMultipartUploadResult initResponse = amazonS3Client.initiateMultipartUpload(initRequest);

				uploadId = initResponse.getUploadId();

				{

					{

						try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileUpldId))) {
							writer.write(uploadId);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}

				}
			} else {

				try {
					uploadId = Files.readAllLines(fileUpldId.toPath()).get(0);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			final long contentLength = file.length();
			long partSize = 100L * 1024L * 1024L; // min 5 MB? vagy összesen min 5 MB?

			boolean didUploadSomething = false;

			long uploadTotal = 0;

			// Upload the file parts.
			long filePosition = 0;
			for (int i = 1; filePosition < contentLength; i++) {

				if (i <= alreadyUploadedCount) {

					System.out.println("upload, part (skip, already uploaded): " + i);

				} else {

					System.out.println("upload, part: " + i);

					// Because the last part could be less than 5 MB, adjust the part size as needed.
					partSize = Math.min(partSize, (contentLength - filePosition));

					// Create the request to upload a part.
					UploadPartRequest uploadRequest = new UploadPartRequest()
							.withBucketName(bucketName)
							.withKey(keyName)
							.withUploadId(uploadId)
							.withPartNumber(i)
							.withFileOffset(filePosition)
							.withFile(file)
							.withPartSize(partSize);

					// Upload the part and add the response's ETag to our list.
					UploadPartResult uploadResult = amazonS3Client.uploadPart(uploadRequest);
					partETags.add(uploadResult.getPartETag());

					didUploadSomething = true;

					{

						String json = gson.toJson(partETags);
						try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileJsonDescriptor))) {
							writer.write(json);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}

					uploadTotal += partSize;

				}

				filePosition += partSize;

			}

			if (didUploadSomething && filePosition >= contentLength) {

				System.out.println("close upload, uploadId: " + uploadId);

				// Complete the multipart upload.
				CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(bucketName, keyName,
						uploadId, partETags);
				amazonS3Client.completeMultipartUpload(compRequest);

				double elapsedSec = (System.currentTimeMillis() - startTs) / 1000d;

				double mbPerSec = uploadTotal / 1024d / 1024d / elapsedSec;

				DecimalFormat formatter = new DecimalFormat("###,###.00");
				formatter.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));

				System.out.println("elapsedSec: " + formatter.format(elapsedSec));
				System.out.println("uploadTotal MB: " + formatter.format(uploadTotal / 1024d / 1024d));
				System.out.println("speed MB/s: " + formatter.format(mbPerSec));

			} else {
				System.out.println("close upload skip (already closed), uploadId: " + uploadId);
			}

		}

		// ---

	}

	private static Regions getRegion(final String regionName) {

		for (final Regions reg : Regions.values()) {
			if (reg.getName().equalsIgnoreCase(regionName)) {
				return reg;
			}
		}

		return Regions.DEFAULT_REGION;
	}

}
