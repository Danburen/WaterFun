package org.waterwood.waterfunservice.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.waterwood.waterfunservice.entity.SalesData;

public interface DashboardRepo extends JpaRepository<SalesData,String> {
}
