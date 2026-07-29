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

    @Mapping(target = "tnId", ignore = true)
    @Mapping(target = "inOut", constant = "3")
    @Mapping(target = "codeAdm", expression = "java(properties.getDefaultCodeAdm())")
    @Mapping(target = "custCode", ignore = true)
    @Mapping(target = "fioAskr", ignore = true)
    @Mapping(target = "ndogBillingA", source = "dto.ndogBillingA")
    @Mapping(target = "accountA", source = "dto.accountA")
    @Mapping(target = "ndogBillingB", source = "dto.ndogBillingB")
    @Mapping(target = "fioBillingA", source = "dto.fioBillingA")
    @Mapping(target = "billDate", source = "dto.billDate")
    @Mapping(target = "typeServA", expression = "java(properties.getDefaultTypeServA())")
    @Mapping(target = "operation", constant = "3")
    @Mapping(target = "summa", source = "dto.summa", qualifiedByName = "toStoredSumma")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "dateInput", ignore = true)
    @Mapping(target = "dateMod", ignore = true)
    @Mapping(target = "codOper", constant = "20000")
    @Mapping(target = "comment", ignore = true)
    @Mapping(target = "billTypeA", constant = "2")
    @Mapping(target = "typeEnter", constant = "0")
    @Mapping(target = "flFile", source = "fileName")
    public abstract TransferBalance toEntity(TransferRecordDto dto, String fileName);

    public abstract TransferStatusResponse toStatusResponse(TransferLoadLog log);

    @Named("toStoredSumma")
    protected BigDecimal toStoredSumma(BigDecimal summa) {
        if (summa == null) {
            return BigDecimal.ZERO;
        }
        return summa.movePointRight(properties.getSummaScale())
                .setScale(0, RoundingMode.HALF_UP);
    }
}
