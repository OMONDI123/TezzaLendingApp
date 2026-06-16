package co.ke.tezza.loanapp.entity;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.*;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.envers.Audited;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AD_Menu")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Audited
public class MADMenu extends AuditModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AD_Menu_ID")
    private long id;

    @Column(name = "menu_icon")
    private String menuIcon;

    private String title;
    
    private long menuOrder;
    @Column(columnDefinition  = "BIGINT DEFAULT 0")
    private long externalMenuId;
    
    @Column(columnDefinition  = "BIGINT DEFAULT 0")
    private long externalClientId;


    @ManyToMany(cascade = { CascadeType.MERGE, CascadeType.PERSIST },fetch = FetchType.EAGER)
    @JoinTable(
        name = "AD_Menu_SubMenu",
        joinColumns = @JoinColumn(name = "AD_Menu_ID"),
        inverseJoinColumns = @JoinColumn(name = "AD_Sub_Menu_ID")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<MADSubMenu> children = new HashSet<>();

    @Column(name = "AD_Menu_UU")
    private String AD_Menu_UU;
}
