package com.binava.stafffinance.loan.service;
import com.binava.stafffinance.loan.entity.LoanCategory;
import com.binava.stafffinance.loan.dto.CategoryRequest;
import com.binava.stafffinance.loan.repository.LoanCategoryRepository;
import com.binava.stafffinance.auth.service.AuthService;
import com.binava.stafffinance.audit.service.AuditService;
import com.binava.stafffinance.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class LoanCategoryService {
 private final LoanCategoryRepository repository; private final AuthService auth; private final AuditService audit;
 public LoanCategoryService(LoanCategoryRepository repository, AuthService auth, AuditService audit) { this.repository=repository; this.auth=auth; this.audit=audit; }
 @Transactional(readOnly=true) public List<LoanCategory> list(boolean activeOnly) { return repository.findAll().stream().filter(c -> !activeOnly || c.active).toList(); }
 @Transactional public LoanCategory save(Long id, CategoryRequest request) {
  var category=id==null?new LoanCategory():repository.findById(id).orElseThrow(() -> new NotFoundException("Category not found"));
  var user=auth.currentUser(); if(id==null) category.createdBy=user.fullName;
  category.name=request.name().trim(); category.annualRate=request.annualRate(); category.description=request.description();
  category.active=request.active()==null?category.active:request.active(); category.modifiedBy=user.fullName;
  repository.save(category); audit.log(user,id==null?"CREATE":"UPDATE","LOAN_CATEGORY",category.id,category.name,null,category.active?"ACTIVE":"INACTIVE","Annual rate="+category.annualRate);
  return category;
 }
}
