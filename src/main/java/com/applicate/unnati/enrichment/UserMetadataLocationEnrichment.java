package com.applicate.unnati.enrichment;

import com.salescode.dim.etl.OperationResult;
import com.salescode.dim.etl.enrichment.AbstractEnrichment;
import com.salescode.dim.jooq.impl.UserMetadata;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class UserMetadataLocationEnrichment extends AbstractEnrichment<UserMetadata> {

	private static final int SCALE = 8;
	private final GeometryFactory geometryFactory = new GeometryFactory();

	@Override
	public OperationResult.StepResult apply(UserMetadata userMetadata) {

		if (userMetadata.getLocation() == null) {
			BigDecimal latitude = getLatitude(userMetadata);
			BigDecimal longitude = getLongitude(userMetadata);

			// Convert BigDecimal → double (required for Coordinate)
			double latValue = latitude.doubleValue();
			double lonValue = longitude.doubleValue();

			Point point = geometryFactory.createPoint(new Coordinate(lonValue, latValue));
			userMetadata.setLocation(point);
		}

		return new OperationResult.StepResult(OperationResult.Status.OK, "Location enriched successfully");
	}

	private BigDecimal getLatitude(UserMetadata userMetadata) {
		BigDecimal latitude = userMetadata.getLatitude();
		return latitude == null ? BigDecimal.ZERO.setScale(SCALE) : latitude.setScale(SCALE, RoundingMode.HALF_UP);
	}

	private BigDecimal getLongitude(UserMetadata userMetadata) {
		BigDecimal longitude = userMetadata.getLongitude();
		return longitude == null ? BigDecimal.ZERO.setScale(SCALE) : longitude.setScale(SCALE, RoundingMode.HALF_UP);
	}
}
