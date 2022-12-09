package searchengine.model;

import com.sun.istack.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table(name = "index_search")
@NoArgsConstructor
@Setter
@Getter
public class IndexSearch {
    @Id
    @NotNull
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @NotNull
    @Column(name = "page_id")
    private int pageId;
    @NotNull
    @Column(name = "lemma_id")
    private int lemmaId;
    @NotNull
    private float lemmaCount;
    @ManyToOne
    @JoinColumn(name = "page_id",insertable = false,updatable = false,nullable = false)
    private Page page;
    @ManyToOne
    @JoinColumn(name = "lemma_id",insertable = false,updatable = false,nullable = false)
    private Lemma lemma;
}