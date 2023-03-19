package hu.lanoga.toolbox.geoarea;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.domain.Page;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Service;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.filter.internal.BasePageRequest;
import hu.lanoga.toolbox.service.ToolboxCrudService;

@ConditionalOnMissingBean(name = "geoAreaServiceOverrideBean")
@Service
public class GeoAreaService implements ToolboxCrudService<GeoArea> {

	@Autowired
	private GeoAreaJdbcRepository geoAreaJdbcRepository;
	
	public String buildGeoReadablePath(final int geoAreaId) {

		GeoArea geo = this.findOne(geoAreaId);

		String country = null;
		String city = null;
		String street = null;

		{

			int gt = geo.getGeoAreaType().intValue();

			if (gt == ToolboxSysKeys.GeoAreaTypes.COUNTRY) {
				country = geo.getGeoAreaName();
			} else if (gt == ToolboxSysKeys.GeoAreaTypes.CITY) {
				city = geo.getGeoAreaName();
			} else if (gt == ToolboxSysKeys.GeoAreaTypes.STREET) {
				street = geo.getGeoAreaName();
			}

		}

		while (geo.getParentArea() != null) {

			geo = this.findOne(geo.getParentArea());

			int gt = geo.getGeoAreaType().intValue();

			if (gt == ToolboxSysKeys.GeoAreaTypes.COUNTRY) {
				country = geo.getGeoAreaName();
			} else if (gt == ToolboxSysKeys.GeoAreaTypes.CITY) {
				city = geo.getGeoAreaName();
			} else if (gt == ToolboxSysKeys.GeoAreaTypes.STREET) {
				street = geo.getGeoAreaName();
			}

		}

		final StringBuilder sbLocationCaption = new StringBuilder();

		if (street == null) {
			sbLocationCaption.append(country + ", " + city);
		} else {
			sbLocationCaption.append(country + ", " + city + ", " + street);
		}

		return sbLocationCaption.toString();
	}

	@Override
	public List<GeoArea> findAll() {
		return geoAreaJdbcRepository.findAll();
	}

	public List<GeoArea> findAllSettlements() {
		return geoAreaJdbcRepository.findAllSettlements();
	}

	public List<GeoArea> findAllSettlementsByParent(final Integer parentAreaId) {
		return geoAreaJdbcRepository.findAllSettlementsByParent(parentAreaId);
	}

	public List<GeoArea> findAllCountries() {
		return geoAreaJdbcRepository.findAllCountries();
	}

	public List<GeoArea> findAllStreets() {
		return geoAreaJdbcRepository.findAllStreets();
	}

	public List<GeoArea> findAllStreetsByParent(final Integer parentAreaId) {
		return geoAreaJdbcRepository.findAllStreetsByParent(parentAreaId);
	}

	@Override
	public GeoArea findOne(final int geoAreaId) {
		return geoAreaJdbcRepository.findOne(geoAreaId);
	}

	public GeoArea findGeoAreaByName(final String geoAreaName) {
		return geoAreaJdbcRepository.findOneBy("geoAreaName", geoAreaName);
	}

	public GeoArea findGeoAreaByUrlName(final String geoAreaUrlName) {
		return geoAreaJdbcRepository.findOneBy("geoAreaUrlName", geoAreaUrlName);
	}

	public GeoArea findGeoAreaByCountryCode(final String countryCode) {
		return geoAreaJdbcRepository.findOneBy("countryCode", countryCode);
	}

	public GeoArea findGeoAreaByPostalCode(final String postalCode) {
		return geoAreaJdbcRepository.findOneBy("postalCode", postalCode);
	}

	public List<GeoArea> findGeoAreasByParentArea(final Integer parentArea) {
		return geoAreaJdbcRepository.findAllBy("parentArea", parentArea);
	}

	@Override
	public long count() {
		return geoAreaJdbcRepository.count();
	}

	@Override
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR)
	public GeoArea save(final GeoArea geoArea) {
		throw new UnsupportedOperationException("Save method is not supported in GeoAreaService.");
	}

	@Override
	@Secured(ToolboxSysKeys.UserAuth.ROLE_ADMIN_STR)
	public void delete(final int geoAreaId) {
		throw new UnsupportedOperationException("Delete method is not supported in GeoAreaService.");
	}

	@Override
	public Page<GeoArea> findAll(final BasePageRequest<GeoArea> pageRequest) {
		throw new UnsupportedOperationException("FindAll method with BasePageRequest is not supported.");
	}
}
