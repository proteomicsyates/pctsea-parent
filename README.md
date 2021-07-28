# Proteomics Cell Type Enrichment Analysis (PCTSEA)

This tool has been designed to be a cell type enrichment tool for proteomics quantitative data.   
It compares publicly available RNAseq single cell datasets with the input protein list and performs an enrichment analysis based on Kolmogorov-Smirnov statistics to end up determining the set of cell types that are significantly enriched in the input data.   
   
### Availability of the software:
PCTSEA server can be found at http://pctsea.scripps.edu   
PCTSEA is also available as a command line at http://sealion.scripps.edu/pCtSEA/  
Source code of PCTSEA can be found in this GitHub repository: https://github.com/proteomicsyates/pctsea-parent   
 - [pctsea-core](https://github.com/proteomicsyates/pctsea-parent/tree/main/pctsea-core): source code of the core of its functionality
 - [pctsea-cl](https://github.com/proteomicsyates/pctsea-parent/tree/main/pctsea-cl): command-line version of the tool
 - [pctseaweb](https://github.com/proteomicsyates/pctsea-parent/tree/main/pctseaweb): web version of the tool  
 - [pctsea-results-viewer] (https://github.com/proteomicsyates/pctsea-parent/tree/main/pctseaweb/shinyR): Shiny app for visualization of results.
 - [pctsea-results-comparator] (https://github.com/proteomicsyates/pctsea-parent/tree/main/pctseaweb/shinyR_comparison): Shiny app for comparison of results.
   
   
### BioRXiv publication:
https://doi.org/10.1101/2021.02.16.431318   
(Posted February 16, 2021)

## FLow chart:   
![Flow chart](https://github.com/proteomicsyates/pctsea-parent/raw/main/docs/flow_chart.png)

## Implementation description:
pctsea-cl and pctseaweb modules are build with Spring Boot framework and pctseaweb with [Vaadin](https://vaadin.com/).  
There is a database storing the single cell expression values, implemented in [MongoDB](https://www.mongodb.com/).   




