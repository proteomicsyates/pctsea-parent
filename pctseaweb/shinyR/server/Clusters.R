genes_file <- reactiveVal()
# select the enrichment file
observeEvent(rv$unziped_files,{
  folder <- rv$unziped_files
  # folder <-list.dirs(folder, recursive = FALSE)[1] # go one folder up
  files <- list.files(folder, pattern = ".*genes.txt")
  if(length(files) > 0){
    file <- paste(folder, .Platform$file.sep, files[1], sep = "")
    genes_file(file)
  }
})


output$heatmap <- renderPlotly({
  req(genes_file())
  table <- fread(file = genes_file(), header = TRUE, sep = "\t")
  table2 <- enrichment_table()
  threshold = input$threshold
  significant_cell_types <- table2[table2$"KS p-value BH corrected"< threshold & table2$ews>0,]$"cell type"

  cell_types <- unique(table$cell_type)
  genes <- unique(table$gene)

  data <- data.frame(matrix(ncol=length(significant_cell_types)))
  colnames(data) <- significant_cell_types

  # "binary", "num_cells", "pct_cells"
  data_type <- input$heatmap_cell_value_type
  for(i in seq(1:nrow(table))){
    row <- as.character(as.vector(table[i,]))
    cell_type<-row[1]
    if (!cell_type %in% significant_cell_types){
      next
    }
    gene <- row[2]
    occurrences <- row[3]
    if(data_type == "binary"){
      data[gene, cell_type] <- 1
    }else if(data_type == "num_cells"){
      data[gene, cell_type] <- as.numeric(occurrences)
    }else if (data_type == "pct_cells"){
      # first check whether we have that info in the table
      if (length(row) < 4){
        showModal(
          modalDialog(title = "Not supported", "This is not supported yet")
          # This run doesn't have the percentage of cells of each type in which the gene is present.
                      # However, if you run it again, you will be able to select this option.")
        )
        return (NULL)
      }
      pct <- row[4]
      data[gene, cell_type] <- as.numeric(pct)
    }
    if (i==1){ # remove first row that is empty in the first iteration
      data <- data[-1,]
    }
  }
  # set NAs to 0
  data[is.na(data)] <- 0

  xlab <- "cell types"
  ylab <- "genes"

  # do we swap columns with rows?
  if (input$swap_rows_cols == "rows"){
    data <- t(data)
    xlab <- "genes"
    ylab <- "cell types"
  }
  title <- ""
  if(input$cluster_genes){
    dendrogram <- "both"
  }else{
    if (input$swap_rows_cols == "rows"){
      dendrogram <- "row"
    }else{
      dendrogram <- "column"
    }
  }

  k_col = 1
  k_row = 1
  num_clusters <- input$num_clusters

  row_dend  <- data %>%
    dist %>%
    hclust %>%
    as.dendrogram
  k = num_clusters
  if (num_clusters == "Automatic"){
    k <- find_k(row_dend)$k
  }
  row_dend  <- row_dend %>%
    set("branches_k_color", k = k) %>%
    ladderize

  col_dend  <- data %>%
    t %>%
    dist %>%
    hclust %>%
    as.dendrogram
  k = num_clusters
  if (num_clusters == "Automatic"){
    k <- find_k(col_dend)$k
  }
  col_dend  <- col_dend %>%
    set("branches_k_color", k = k)  %>%
    ladderize

  if (num_clusters == "Automatic" & input$swap_rows_cols == "rows"){
    k_col = "NA"
    k_row = "NA"
  }


  width <- input$heatmap_width
  height <- input$heatmap_height
  font_size <- input$heatmap_font_size
  withProgress({
    p <- heatmaply::heatmaply(data,
                              main = title,
                              xlab = xlab,
                              ylab = ylab,
                              Rowv = row_dend,
                              Colv = col_dend,
                              # k_row = k_row,
                              # k_col = k_col,
                              # row_text_angle = input$row_text_angle,
                              # column_text_angle = input$column_text_angle,
                              dendrogram = dendrogram,
                              # branches_lwd = input$branches_lwd,
                              # seriate = input$seriation,
                              # colors=eval(parse(text=paste0(input$pal,'(',input$ncol,')'))),
                              # distfun_row =  distfun_row,
                              # hclustfun_row = hclustfun_row,
                              # distfun_col = distfun_col,
                              # hclustfun_col = hclustfun_col,
                              # k_col = input$c,
                              # k_row = input$r,
                              # limits = ColLimits
                              fontsize_row = font_size,
                              fontsize_col = font_size
    ) %>%
      layout(width=width, height=height)
    # plotly::layout(margin = list(l = input$l, b = input$b))
  }, message = "Creating heatmap...", detail = "Please wait a second...")
  return (p)
})