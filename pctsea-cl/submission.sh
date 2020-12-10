#!/bin/bash
#SBATCH --nodes=1
#SBATCH --cpus-per-task=8
#SBATCH --mem=16G
#SBATCH --time=2:00:00
##SBATCH --output=job.%A_%a.out #/dev/null to cancel output file
##SBATCH --error=job.%A_%a.err #remove line to join with output or /dev/null to cancel error file
#SBATCH --output=/dev/null
#SBATCH --job-name=pctsea_test
##SBATCH --array=120-179
#SBATCH --array=120-121

cd $SLURM_SUBMIT_DIR
module load java
#echo "My SLURM ARRAY TASK ID is" ${SLURM_ARRAY_TASK_ID}
java -jar pctsea-cl*.jar --spring.data.mongodb.host=sealion.scripps.edu -perm 100 -eef pctsea_input_Line_${SLURM_ARRAY_TASK_ID}.txt  -min_cells_per_cell_type 20 -min_correlation 0.1 -out ${SLURM_ARRAY_TASK_ID} -min_genes_cells 7 -charts

