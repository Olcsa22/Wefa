package hu.lanoga.toolbox.amazon.s3;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryManager;
import hu.lanoga.toolbox.tenant.TenantKeyValueSettings;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.tenant.TenantKeyValueSettingsService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnMissingBean(name = "amazonS3ManagerOverrideBean")
public class AmazonS3Manager {

	/**
	 * más S3 API kompatiblis szerverhez (minio-ra előkszítve most)
	 */
	@Value("${tools.amazon.s3.endpoint}")
	private String endpointUrl;
	
	/**
	 * "rendes" Amazon S3-hoz (ekkor endpointUrl nem kell)
	 */
	@Value("${tools.amazon.s3.region-name}")
	private String regionName;

	@Value("${tools.amazon.s3.bucket-name}")
	private String bucketName;

	@Value("${tools.amazon.s3.access-key}")
	private String accessKey;

	@Value("${tools.amazon.s3.secret-key}")
	private String secretKey;

	@Autowired
	private TenantKeyValueSettingsService tenantKeyValueSettingsService;

	/**
	 * tenantId -> AmazonS3 bucket name
	 */
	private final ConcurrentHashMap<Integer, String> bucketNameMap = new ConcurrentHashMap<>();

	/**
	 * tenantId -> AmazonS3 client
	 */
	private final LoadingCache<Integer, AmazonS3> s3ClientHolder = CacheBuilder.newBuilder().concurrencyLevel(4).maximumSize(100).expireAfterWrite(3, TimeUnit.HOURS).build(new CacheLoader<Integer, AmazonS3>() {

		@Override
		public AmazonS3 load(final Integer key) throws Exception {

			// SecurityUtil.limitAccessSameTenant(key); // ez nem megy itt, mert ebben a tl tenant id is benne van... TODO: tisztázni kell az egész ügyet

			String localBucketName = AmazonS3Manager.this.bucketName;
			String localRegionName = AmazonS3Manager.this.regionName;
			String localAccessKey = AmazonS3Manager.this.accessKey;
			String localSecretKey = AmazonS3Manager.this.secretKey;

			final TenantKeyValueSettings kvBucketName = tenantKeyValueSettingsService.findOneByKey(ToolboxSysKeys.TenantKeyValueSettings.TOOLS_AMAZON_S3_BUCKET_NAME);
			final TenantKeyValueSettings kvRegionName = tenantKeyValueSettingsService.findOneByKey(ToolboxSysKeys.TenantKeyValueSettings.TOOLS_AMAZON_S3_REGION_NAME);
			final TenantKeyValueSettings kvLocalAccessKey = tenantKeyValueSettingsService.findOneByKey(ToolboxSysKeys.TenantKeyValueSettings.TOOLS_AMAZON_S3_ACCESS_KEY);
			final TenantKeyValueSettings kvLocalSecretKey = tenantKeyValueSettingsService.findOneByKey(ToolboxSysKeys.TenantKeyValueSettings.TOOLS_AMAZON_S3_SECRET_KEY);

			if (TenantKeyValueSettingsService.checkIfSettingIsValid(kvBucketName)) {
				localBucketName = kvBucketName.getKvValue();
			}

			if (TenantKeyValueSettingsService.checkIfSettingsAreValid(kvRegionName, kvLocalAccessKey, kvLocalSecretKey)) {
				localRegionName = kvRegionName.getKvValue();
				localAccessKey = kvLocalAccessKey.getKvValue();
				localSecretKey = kvLocalSecretKey.getKvValue();
			}

			AmazonS3Manager.this.bucketNameMap.put(key, localBucketName);

			AmazonS3ClientBuilder amazonS3ClientBuilder = AmazonS3ClientBuilder.standard();

			if (StringUtils.isNotBlank(endpointUrl)) {
				ClientConfiguration clientConfiguration = new ClientConfiguration();
				clientConfiguration.setSignerOverride("AWSS3V4SignerType");
				amazonS3ClientBuilder = amazonS3ClientBuilder.withClientConfiguration(clientConfiguration);
				amazonS3ClientBuilder = amazonS3ClientBuilder.withPathStyleAccessEnabled(Boolean.TRUE);
				amazonS3ClientBuilder = amazonS3ClientBuilder.withEndpointConfiguration(new EndpointConfiguration(endpointUrl, null));
			} else {
				amazonS3ClientBuilder = amazonS3ClientBuilder.withRegion(getRegion(localRegionName));
			}	
			
			amazonS3ClientBuilder = amazonS3ClientBuilder.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(localAccessKey, localSecretKey)));
			
			return amazonS3ClientBuilder.build();
		}

	});

	private static Regions getRegion(final String regionName) {
		for (final Regions reg : Regions.values()) {
			if (reg.getName().equalsIgnoreCase(regionName)) {
				return reg;
			}
		}

		return Regions.DEFAULT_REGION;
	}

	/**
	 * Fájl feltöltése a bucketbe
	 *
	 * @param file a feltöltendő fájl
	 */
	public void upload(final String key, final File file) {

		try {

			Integer tlTenantId = JdbcRepositoryManager.getTlTenantId();
			int tenantId = tlTenantId != null ? tlTenantId : SecurityUtil.getLoggedInUserTenantId();

			AmazonS3 amazonS3 = this.s3ClientHolder.get(tenantId);
			String bn = this.bucketNameMap.get(tenantId);

			try {
				if (!amazonS3.doesBucketExistV2(bn)) {
					synchronized (this) {
						if (!amazonS3.doesBucketExistV2(bn)) {
							amazonS3.createBucket(bn);
						}
					}
				}
			} catch (Exception e) {
				log.error("upload, createBucket if not exists failed", e);
			}
	
			amazonS3.putObject(bn, key, file);

		} catch (final Exception e) {
			throw new AmazonS3ManagerException("File upload error!", e);
		}

	}

	/**
	 * @param key
	 * @param file ide lesz mentve (letöltve)
	 */
	public void download(final String key, final File file) {
		try {

			Integer tlTenantId = JdbcRepositoryManager.getTlTenantId();
			int tenantId = tlTenantId != null ? tlTenantId : SecurityUtil.getLoggedInUserTenantId();

			this.s3ClientHolder.get(tenantId).getObject(new GetObjectRequest(this.bucketNameMap.get(tenantId), key), file);

		} catch (final Exception e) {
			throw new AmazonS3ManagerException("File download error!", e);
		}

	}

	/**
	 * Adott fájlhoz tartozó URL legenerálása, melynek birtokában bárki hozzáférhet a fájlhoz
	 *
	 * @param objectKey filename (pl: 'abc.jpg')
	 * @return presigned url
	 */
	public String generatePresignedUrl(final String objectKey) {

		try {

			Integer tlTenantId = JdbcRepositoryManager.getTlTenantId();
			int tenantId = tlTenantId != null ? tlTenantId : SecurityUtil.getLoggedInUserTenantId();

			final GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(this.bucketNameMap.get(tenantId), objectKey).withMethod(HttpMethod.GET).withExpiration(new Date(System.currentTimeMillis() + (1000L * 3600L * 24L * 7L)));
			final URL url = this.s3ClientHolder.get(tenantId).generatePresignedUrl(generatePresignedUrlRequest);

			return url.toString();

		} catch (final Exception e) {
			throw new AmazonS3ManagerException("Generate presigned url error!", e);
		}

	}

}
