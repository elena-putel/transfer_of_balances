package com.beltelecom.transfer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transfer_path")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferPath {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_region", nullable = false)
    private Integer idRegion;

    @Column(name = "path", nullable = false, length = 500)
    private String path;
}
