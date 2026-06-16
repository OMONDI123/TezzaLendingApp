/**
 * 
 */
package co.ke.tezza.loanapp.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.hibernate.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import co.ke.tezza.loanapp.entity.MClient;
import co.ke.tezza.loanapp.entity.MOrg;
import co.ke.tezza.loanapp.entity.MRoles;
import co.ke.tezza.loanapp.entity.MUser;
import co.ke.tezza.loanapp.enums.ApprovalStage;
import co.ke.tezza.loanapp.enums.DocStatus;
import co.ke.tezza.loanapp.exceptions.SetUpExceptions;
import co.ke.tezza.loanapp.model.RoleModel;
import co.ke.tezza.loanapp.repository.MOrgRepository;
import co.ke.tezza.loanapp.repository.MclientRepository;
import co.ke.tezza.loanapp.repository.RoleRepository;
import co.ke.tezza.loanapp.response.RoleResponse;
import co.ke.tezza.loanapp.util.Utils;

/**
 * @author austine
 *
 */
@Service
public class RoleServiceImpl implements RoleService {
	@Autowired
	private RoleRepository repository;

	@Autowired
	private Utils utils;

	@Autowired
	private MclientRepository mclientRepository;
	@Autowired
	private MOrgRepository mOrgRepository;

	@Override
	public MRoles getRolesById(Long role_id) throws SetUpExceptions {
		// TODO Auto-generated method stub
		try {
			Optional<MRoles> user = repository.findById(role_id);
			if (user.isPresent()) {
				return user.get();
			} else {
				throw new SetUpExceptions("Failed to fetch role with ID");
			}
		} catch (Exception e) {
			// TODO: handle exception
			throw new SetUpExceptions("Failed to fetch role with ID");
		}
	}

	@Override
	public RoleResponse createRoles(RoleModel role) throws SetUpExceptions {
		// TODO Auto-generated method stub
		try {
			MRoles mRoles = repository.findById(role.getId()).orElse(null);
			if (mRoles == null) {
				mRoles = new MRoles();
			}

			mRoles.setActive(true);
			mRoles.setFormattedName(Utils.toReadableFormat(role.getRoleName()));
			mRoles.setName(role.getRoleName());
			mRoles.setDescription(role.getRoleDescription());
			mRoles.setApprovalStage(ApprovalStage.APPROVED);
			mRoles.setApproved(true);
			mRoles.setAllowMultiOrgAccess(role.isAllowMultiOrgAccess());
			Set<MOrg> allowedOrganisations = new HashSet<>();
			if(role.getAllowedOrganisationIds().size()>0) {
				for (Long id :role.getAllowedOrganisationIds()) {
					MOrg org= mOrgRepository.findById(id).orElse(null);
					if(org!=null) {
						allowedOrganisations.add(org);
					}
				}
			}
			mRoles.setAllowedOrganisations(allowedOrganisations);			
			repository.save(mRoles);

			mRoles.setDocumentNo("ROLE/NO/" + Utils.getCurrentYear() + "/" + mRoles.getId());

			mRoles.setDocStatus(DocStatus.APPROVED);
			return utils.mapRole(repository.save(mRoles));
		} catch (Exception e) {
			// TODO: handle exception
			throw new SetUpExceptions("Could not create role");
		}
	}

	@Override
	public MRoles deleteById(Long role_id) throws SetUpExceptions {
		// TODO Auto-generated method stub
		try {
			MRoles existEntity = repository.findById(role_id).orElse(null);

			if (existEntity == null) {
				throw new SetUpExceptions("Role is not found");

			}
			existEntity.setActive(false);
			return repository.save(existEntity);
		} catch (ObjectNotFoundException e) {
			throw new SetUpExceptions("Could not find Role by Id");
			// TODO: handle exception
		}
	}

	@Override
	public List<MRoles> getAllRoles() throws SetUpExceptions {
		// TODO Auto-generated method stub
		try {
			return repository.findAll();
		} catch (Exception e) {
			// TODO: handle exception
			throw new SetUpExceptions("Failed to fetch all the roles");
		}
	}

	@Override
	public Page<MRoles> findByIsActiveAndAdOrgIDOrAndSuperAdmin(boolean isActive, int page, int size)
			throws SetUpExceptions {
		// TODO Auto-generated method stub
		try {
			Page<MRoles> roles = repository.findByIsActiveAndAdOrgIDOrAdOrgIDOrderByIdAsc(isActive,
					utils.getAD_Org_ID(), utils.getAD_Org_IDRole(), PageRequest.of(page, size));
			if (roles.hasContent()) {
				for (MRoles role : roles) {
					role.setClientName(mclientRepository.findById(role.getAdClientId()).get().getName());
					role.setOrganizationName(mOrgRepository.findById(role.getAdOrgID()).get().getName());
				}
			}
			return roles;
		} catch (Exception e) {
			// TODO: handle exception
			throw new SetUpExceptions("Could not fetch active roles" + e);
		}
	}

	@Override
	public Page<MRoles> findByIsActiveAndAdOrgID(boolean isActive, int page, int size) throws SetUpExceptions {
		// TODO Auto-generated method stub
		try {
			Page<MRoles> roles = null;
			if (utils.isSuperUser()) {
				roles = repository.findByIsActiveAndAdOrgIDOrAdOrgIDOrderByIdAsc(isActive, utils.getAD_Org_ID(),
						utils.getAD_Org_IDRole(), PageRequest.of(page, size));

			} else {
				roles = repository.findByIsActiveAndAdClientIdOrderByIdAsc(isActive, utils.getAD_Client_ID(),
						PageRequest.of(page, size));

			}

			if (roles.hasContent()) {
				for (MRoles role : roles) {
					MClient client=mclientRepository.findById(role.getAdClientId()).orElse(null);
					if(client!=null) {
						role.setClientName(client.getName());
					}
					MOrg org=mOrgRepository.findById(role.getAdOrgID()).orElse(null);
					if(org!=null) {
						role.setOrganizationName(org.getName());
					}
					
					
				}
			}
			return roles;
		} catch (Exception e) {
			// TODO: handle exception
			throw new SetUpExceptions("Could not fetch active roles" + e);
		}
	}

	@Override
	public List<MRoles> getRolesByUserId() {
		// TODO Auto-generated method stub
		MUser user = utils.getLoggedInUser();
		List<MRoles> roles = new ArrayList<>();
		if (user != null) {
			if (user.getRoles().size() > 0) {
				roles = new ArrayList<>(user.getRoles());
			}

		}

		return roles;
	}

}
