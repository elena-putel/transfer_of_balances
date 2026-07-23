package com.beltelecom.transfer.mapper;

import com.beltelecom.transfer.config.TransferProperties;
import com.beltelecom.transfer.dto.TransferRecordDto;
import com.beltelecom.transfer.dto.TransferStatusResponse;
import com.beltelecom.transfer.entity.TransferBalance;
import com.beltelecom.transfer.entity.TransferLoadLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Mapper(componentModel = "spring")
public abstract class TransferMapper {

    @Autowired
    protected TransferProperties properties;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "custCode", expression = "java(properties.getDefaultCustCode())")
    @Mapping(target = "fioAskr", source = "dto.fioBillingA")
    @Mapping(target = "ndogBillingA", source = "dto.ndogBillingA")
    @Mapping(target = "accountA", source = "dto.accountA")
    @Mapping(target = "ndogBillingB", source = "dto.ndogBillingB")
    @Mapping(target = "fioBillingA", source = "dto.fioBillingA")
    @Mapping(target = "billDate", source = "dto.billDate")
    @Mapping(target = "typeServA", expression = "java(properties.getDefaultTypeServA())")
    @Mapping(target = "operation", source = "dto.summa", qualifiedByName = "resolveOperation")
    @Mapping(target = "summa", source = "dto.summa", qualifiedByName = "toStoredSumma")
    @Mapping(target = "status", constant = "1")
    @Mapping(target = "dateInput", ignore = true)
    @Mapping(target = "dateMod", ignore = true)
    @Mapping(target = "codOper", expression = "java(properties.getDefaultCodOper())")
    @Mapping(target = "comment", ignore = true)
    @Mapping(target = "billTypeA", ignore = true)
    @Mapping(target = "typeEnter", constant = "0")
    @Mapping(target = "flFile", source = "fileName")
    public abstract TransferBalance toEntity(TransferRecordDto dto, String fileName);

    public abstract TransferStatusResponse toStatusResponse(TransferLoadLog log);

    @Named("resolveOperation")
    protected Short resolveOperation(BigDecimal summa) {
        if (summa == null || summa.signum() == 0) {
            return 0;
        }
        return summa.signum() > 0 ? (short) 1 : (short) 2;
    }

    @Named("toStoredSumma")
    protected BigDecimal toStoredSumma(BigDecimal summa) {
        if (summa == null) {
            return BigDecimal.ZERO;
        }
        return summa.movePointRight(properties.getSummaScale())
                .setScale(0, RoundingMode.HALF_UP);
    }
}
