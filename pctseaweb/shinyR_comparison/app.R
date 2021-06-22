#
# This is a Shiny web application. You can run the application by clicking
# the 'Run App' button above.
#
# Find out more about building applications with Shiny here:
#
#    http://shiny.rstudio.com/
#

library(shiny)
library(morpheus)
library(data.table)
library(htmlwidgets) 
library(jsonlite)
library(dplyr)
library(devtools)
library(ggplot2)
# devtools::install_github('ramnathv/htmlwidgets')
# if(!require('devtools')){
# install.packages('devtools')
# }
# library(devtools)
# devtools::install_github('cmap/morpheus.R')

# read table
# setwd("C:\\Users\\salvador\\Dropbox (Scripps Research)\\NCI60_pctsea")
# data <- read.csv(file = "SignificantCellTypesTable_FDR.txt", sep = "\t")
# rownames(data) <- data[,1]
# data[,1]<-NULL
# data[is.na(data)] <- 1.0
# data[data==0.0] <- 0.0000000000000000001
# data <- log10(data)
# y <- data.frame(TumorOrigin=annotations$TumorOrigin)
#setwd("C:\\Users\\salvador\\Dropbox (Scripps Research)\\NCI60_pctsea\\NCI60_PCTSEA")
# annotations <- read.csv(file = "data/CellLineID_to_LineNumber.txt", sep = "\t")

data_types = c('hyperG_p-value','log2_ratio','ews','norm-ews','supX','norm-supX','empirical_p-value','FDR','2nd_ews','2nd_supX','Dab','KS_p-value','KS_p-value_BH_corrected','KS_significance_level')

# Define UI for application that draws a histogram
ui <- fluidPage(
  # Application title
  titlePanel("Comparison of PCTSEA results"),
  tabsetPanel(
    tabPanel("Heatmap",
             # Sidebar with a slider input for number of bins
             sidebarLayout(
               sidebarPanel(
                 width = 2,
                 selectInput(inputId = "dataTypeHeatmap", label = "Data type", choices = c("", data_types), selected = data_types[8]),
                 numericInput(inputId = "fdrThreshold", label ="FDR threshold:", value = 0.05),
                 checkboxInput(inputId = "clusterCellTypes", label = "Cluster cell types"),
                 checkboxInput(inputId = "clusterCellLines", label = "Cluster samples"),
                 checkboxInput(inputId = "doLog10", label = "Transform log10"),
                 checkboxInput(inputId = "scalingMode", label = "Color scale independent per cell type (horizontally)"),
                 numericInput(inputId = "naValues", label ="Replace NaN values with:", value = NULL),
                 downloadButton("downloadData", label = "Download table (not filtered)")
               ),
               # Show a plot of the generated distribution
               mainPanel(
                 width = 10,
                 fluidRow(column(12, verbatimTextOutput(outputId = "errorMessageText"))),
                 fluidRow(column(12, morpheusOutput("heatmap", height = "1600px", width = "1000px")))
               )
             )
    )
  )
)

# Define server logic required to draw a histogram
server <- function(input, output, session) {
  
  data_folder <- reactiveVal()
  observe({
    query <- parseQueryString(session$clientData$url_search)
    if(!is.null(query[['f']])) {
      code <- query[['f']]
      # set code to folder
      data_folder(paste0('data/comparisons/', code))
    }
  })
  output$downloadData <- downloadHandler(
    filename <- function() {
      paste0(data_folder(),"/SignificantCellTypesTable_", input$dataTypeHeatmap ,".txt")
    },
    
    content <- function(file) {
      file.copy(paste0(data_folder(),"/SignificantCellTypesTable_", input$dataTypeHeatmap ,".txt"), file)
    },
    contentType = "text/plain"
  )
  
  
  errorMessage <- reactiveVal()
  
  observeEvent({
    input$dataTypeHeatmap
    input$clusterCellTypes
    input$clusterCellLines
    input$doLog10
    input$naValues
    input$fdrThreshold},
    {
      output$heatmap <- renderMorpheus({
        req(input$dataTypeHeatmap)
        fdr_data <- read.csv(file = paste0(data_folder(),"/SignificantCellTypesTable_FDR.txt"), sep = "\t", row.names = 1)
        if(input$dataTypeHeatmap!='FDR'){
          data <- read.csv(file = paste0(data_folder(),"/SignificantCellTypesTable_", input$dataTypeHeatmap ,".txt"), sep = "\t", row.names = 1)
        }else{
          data <- fdr_data
        }
        if (!is.null(input$fdrThreshold)){
          print(paste("fdr threshold:",input$fdrThreshold))
          fdr_data[fdr_data > input$fdrThreshold] <- NA
          fdr_data<- as.data.frame(t(fdr_data))
          not_all_na <- function(x) any(!is.na(x))
          fdr_data <- fdr_data %>% select(where(not_all_na))
          fdr_data <- as.data.frame(t(fdr_data))
          # now filter data with the rows present in fdr_data
          data <- data[rownames(data) %in% rownames(fdr_data),]
        }
        y <- data.frame(sample = colnames(data), #TumorOrigin=annotations$TumorOrigin,  
                        stringsAsFactors = F)
        # cluster cell types:
        rowv = NULL
        colv = NULL
        if (!is.null(input$naValues)){
          data[is.na(data)] <- input$naValues
        }
        if (input$doLog10){
          data[data==0.0] <- 0.0000000000000000001
          data <- log10(data)
        }
        if (input$clusterCellTypes & input$clusterCellLines){
          dendrogram = "both"
          rowv = TRUE
          colv = TRUE
        }else if (input$clusterCellTypes){
          dendrogram = "row"
          rowv = TRUE
        }else if (input$clusterCellLines){
          dendrogram = "column"
          colv = TRUE
        }else{
          dendrogram = "none"
        }
        colorScheme <- list(scalingMode = "relative")
        if(!input$scalingMode){
          colorScheme <- list(scalingMode = "fixed",min = min(data, na.rm = TRUE), max = max(data, na.rm = TRUE))
        }
        num_infinities <- sum(is.infinite(as.matrix(data)))
        num_nans <- sum(is.na.data.frame(data))
        print(paste('number of cells with infinities:',num_infinities))
        print(paste('number of cells with nan:',num_nans))
        show_error <- FALSE
        if (num_infinities>0 | num_nans>0){
          if(!is.null(colv) | !is.null(rowv)){
            show_error = TRUE
          }
        }
        if(!show_error){
          errorMessage("")
          plot <- morpheus(data,
                           Rowv = rowv,
                           Colv = colv,
                           dendrogram = dendrogram,
                           # na.rm = TRUE,
                           # columnAnnotations = y,
                           # columnGroupBy=list(list(field='TumorOrigin')),
                           # by default color scale is map to the minimum and maximum of each row independently
                           colorScheme=colorScheme,
                           columns=list(list(field='TumorOrigin',display=list('text_and_color')))
                           
          )#+ scale_fill_gradient( trans = 'log' )
          plot
          # morpheus(mtcars,
          #          dendrogram='column',
          #          colorScheme=list(scalingMode="fixed", colors=heat.colors(3)),
          #          rowAnnotations=rowAnnotations,
          #          tools=list(list(name='Hierarchical Clustering', params=list(group_rows_by=list('annotation2'), cluster='Rows'))),
          #          rowGroupBy=list(list(field='annotation2')),
          #          rows=list(list(field='annotation2',display=list('color')))
          #          )
        }else{
          errorMessage("You need to replace NaN values in order to perform the clustering")
          return(NULL)
        }
      })
    })
  observeEvent(errorMessage(),{
    msg <- errorMessage()
    print(msg)
    output$errorMessageText <- renderText({msg})
  }
  )
}


# Run the application
shinyApp(ui = ui, server = server)

# setwd("C:\\Users\\salvador\\Dropbox (Scripps Research)\\NCI60_pctsea\\NCI60_PCTSEA")
# fdr_data <- read.csv(file = paste0("data/SignificantCellTypesTable_FDR.txt"), sep = "\t", row.names = 1)
# fdr_data[fdr_data>0.05] <- NA
# fdr_data<- as.data.frame(t(fdr_data))
# not_all_na <- function(x) any(!is.na(x))
# fdr_data <- fdr_data %>% select(where(not_all_na))
# fdr_data <- as.data.frame(t(fdr_data))
#
# data <- read.csv(file = paste0("data/SignificantCellTypesTable_ews.txt"), sep = "\t", row.names = 1)
# data <- data[rownames(data) %in% rownames(fdr_data),]
