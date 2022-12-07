package searchengine.model;

import com.sun.istack.NotNull;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "lemma")
public class Lemma {
    @Id
    @NotNull
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @NotNull
    @Column(name = "site_id")
    private int siteId;
    @NotNull
    @Column(columnDefinition = "VARCHAR(255)")
    private String lemma;
    @NotNull
    private int frequency;
    @ManyToOne
    @JoinColumn(name = "site_id",insertable = false,updatable = false,nullable = false)
    private Site site;
//    @OneToMany
//    @JoinColumn(name = "lemma_id")
//    private List<IndexSearch> indices;
}
