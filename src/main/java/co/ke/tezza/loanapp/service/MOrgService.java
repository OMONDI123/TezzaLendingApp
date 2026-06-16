package co.ke.tezza.loanapp.service;

import java.util.List;
import java.util.UUID;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import co.ke.tezza.loanapp.entity.MOrg;
import co.ke.tezza.loanapp.exceptions.SetUpExceptions;
import co.ke.tezza.loanapp.model.MOrgModel;
import co.ke.tezza.loanapp.repository.MOrgRepository;
import co.ke.tezza.loanapp.repository.MclientRepository;
import co.ke.tezza.loanapp.util.ResponseEntity;
import co.ke.tezza.loanapp.util.Utils;

@Service
public class MOrgService {

	@Autowired
	private MOrgRepository mOrgRepository;

	@Autowired
	private MclientRepository mclientRepository;
	@Autowired
	private Utils utils;

	public ResponseEntity<MOrg> createOrUpdateOrganisations(@Valid MOrgModel m) throws SetUpExceptions {
		try {
			MOrg org = mOrgRepository.findById(m.getId()).orElse(null);
			String message = "";
			if (org == null) {
				org = new MOrg();

			}
			org.setActive(true);
			org.setName(m.getName());
			if(m.getDescription()==null) {
				org.setDescription(m.getName());
			}
			if(m.getKraPin()==null) {
				org.setValue(m.getName());
			}else {
				org.setValue(m.getKraPin());
			}
			
			org.setKraPin(m.getKraPin());
			org.setLocation(m.getLocation());
			org.setPhysicalAddress(m.getPhysicalAddress());
			org.setBoxNo(m.getBoxNo());
			org.setCounty(m.getCounty());
			org.setCity(m.getCity());
			org.setStreet(m.getStreet());
			org.setLandMark(m.getLandMark());
			org.setAD_Org_UU(UUID.randomUUID().toString());
			System.out.println("Client ID==" + m.getAdClientId());
			mOrgRepository.save(org);
			org.setDocumentNo("AD/ORG/" + Utils.getCurrentYear() + "/" + org.getId());
			mOrgRepository.save(org);
			if (m.getId() == 0) {
				message = "Organisation Created Successfully.";

			} else {
				message = "Organisation Updated Successfully.";

			}

			return new ResponseEntity<MOrg>(message, 200, org);

		} catch (Exception e) {
			// TODO: handle exception
			throw new SetUpExceptions("Could Not create or update Organisation." + e);
		}
	}

	public Page<MOrg> getAllOrganisations(int page, int size) {
		Page<MOrg> pageResult = mOrgRepository.findAll(PageRequest.of(page, size));
		if (pageResult.hasContent()) {
			for (MOrg org : pageResult) {
				org.setClientName(mclientRepository.findById(org.getAdClientId()).get().getName());
			}
		}
		return pageResult;
	}

	public void deleteOrganisationById(long id) {
		mOrgRepository.deleteById(id);
		// TODO Auto-generated method stub

	}

	public List<MOrg> getOrganisationByClientId(long aD_Client_ID) {
		// TODO Auto-generated method stub
		return mOrgRepository.findByIsActiveAndAdClientId(true, aD_Client_ID);
	}

	public List<MOrg> getOrganisationByCurrentClient(String search) {
		// TODO Auto-generated method stub
		if(search!=null && !search.isEmpty()) {
			return mOrgRepository.findByIsActiveAndAdClientIdAndNameContainsIgnoreCase(true,utils.getAD_Client_ID(),search);
		}else {
			return mOrgRepository.findByIsActiveAndAdClientId(true, utils.getAD_Client_ID());

		}
	}

}
