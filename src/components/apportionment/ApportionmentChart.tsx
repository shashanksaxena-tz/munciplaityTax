import React from 'react';
import { BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { ApportionmentBreakdown } from '../services/apportionmentService';

interface ApportionmentChartProps {
  breakdown: ApportionmentBreakdown;
  chartType?: 'bar' | 'pie';
}

/**
 * ApportionmentChart Component
 * 
 * Visualizes apportionment factor breakdown using Recharts.
 * Displays property, payroll, and sales factor contributions to final apportionment.
 * 
 * @param breakdown - Apportionment breakdown data
 * @param chartType - Type of chart to display ('bar' or 'pie')
 */
const ApportionmentChart: React.FC<ApportionmentChartProps> = ({
  breakdown,
  chartType = 'bar'
}) => {
  // Prepare data for charts
  const factorData = [
    {
      name: 'Property Factor',
      percentage: breakdown.propertyFactorPercentage,
      weight: breakdown.propertyFactorWeight,
      contribution: breakdown.propertyFactorWeightedContribution,
      color: '#3B82F6' // blue
    },
    {
      name: 'Payroll Factor',
      percentage: breakdown.payrollFactorPercentage,
      weight: breakdown.payrollFactorWeight,
      contribution: breakdown.payrollFactorWeightedContribution,
      color: '#10B981' // green
    },
    {
      name: 'Sales Factor',
      percentage: breakdown.salesFactorPercentage,
      weight: breakdown.salesFactorWeight,
      contribution: breakdown.salesFactorWeightedContribution,
      color: '#F59E0B' // amber
    }
  ];

  // Custom tooltip for detailed information
  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const data = payload[0].payload;
      return (
        <div className="bg-white p-4 border border-gray-300 rounded shadow-lg">
          <p className="font-semibold text-gray-900">{data.name}</p>
          <p className="text-sm text-gray-700">
            Factor: <span className="font-medium">{data.percentage.toFixed(2)}%</span>
          </p>
          <p className="text-sm text-gray-700">
            Weight: <span className="font-medium">{(data.weight * 100).toFixed(2)}%</span>
          </p>
          <p className="text-sm text-gray-700">
            Contribution: <span className="font-medium">{data.contribution.toFixed(2)}%</span>
          </p>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="w-full space-y-6">
      {/* Chart Title */}
      <div className="text-center">
        <h3 className="text-lg font-semibold text-gray-900">
          Apportionment Factor Breakdown
        </h3>
        <p className="text-sm text-gray-600 mt-1">
          Final Apportionment: {breakdown.finalApportionmentPercentage.toFixed(4)}%
        </p>
      </div>

      {/* Bar Chart */}
      {chartType === 'bar' && (
        <ResponsiveContainer width="100%" height={300}>
          <BarChart
            data={factorData}
            margin={{ top: 20, right: 30, left: 20, bottom: 5 }}
          >
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="name" />
            <YAxis 
              label={{ value: 'Percentage (%)', angle: -90, position: 'insideLeft' }}
            />
            <Tooltip content={<CustomTooltip />} />
            <Legend />
            <Bar 
              dataKey="percentage" 
              fill="#8884d8" 
              name="Factor Percentage"
            >
              {factorData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.color} />
              ))}
            </Bar>
            <Bar 
              dataKey="contribution" 
              fill="#82ca9d" 
              name="Weighted Contribution"
            >
              {factorData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.color} opacity={0.7} />
              ))}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      )}

      {/* Pie Chart */}
      {chartType === 'pie' && (
        <ResponsiveContainer width="100%" height={300}>
          <PieChart>
            <Pie
              data={factorData}
              cx="50%"
              cy="50%"
              labelLine={false}
              label={(entry) => `${entry.name}: ${entry.contribution.toFixed(2)}%`}
              outerRadius={100}
              fill="#8884d8"
              dataKey="contribution"
            >
              {factorData.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={entry.color} />
              ))}
            </Pie>
            <Tooltip content={<CustomTooltip />} />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      )}

      {/* Factor Details Table */}
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-gray-300">
          <thead className="bg-gray-50">
            <tr>
              <th scope="col" className="py-3 pl-4 pr-3 text-left text-sm font-semibold text-gray-900">
                Factor
              </th>
              <th scope="col" className="px-3 py-3 text-right text-sm font-semibold text-gray-900">
                Percentage
              </th>
              <th scope="col" className="px-3 py-3 text-right text-sm font-semibold text-gray-900">
                Weight
              </th>
              <th scope="col" className="px-3 py-3 text-right text-sm font-semibold text-gray-900">
                Contribution
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200 bg-white">
            {factorData.map((factor, index) => (
              <tr key={index}>
                <td className="whitespace-nowrap py-3 pl-4 pr-3 text-sm font-medium text-gray-900">
                  <span 
                    className="inline-block w-3 h-3 rounded-full mr-2"
                    style={{ backgroundColor: factor.color }}
                  />
                  {factor.name}
                </td>
                <td className="whitespace-nowrap px-3 py-3 text-right text-sm text-gray-700">
                  {factor.percentage.toFixed(4)}%
                </td>
                <td className="whitespace-nowrap px-3 py-3 text-right text-sm text-gray-700">
                  {(factor.weight * 100).toFixed(2)}%
                </td>
                <td className="whitespace-nowrap px-3 py-3 text-right text-sm font-medium text-gray-900">
                  {factor.contribution.toFixed(4)}%
                </td>
              </tr>
            ))}
            <tr className="bg-gray-50 font-semibold">
              <td className="whitespace-nowrap py-3 pl-4 pr-3 text-sm text-gray-900">
                Final Apportionment
              </td>
              <td className="whitespace-nowrap px-3 py-3 text-right text-sm text-gray-900" colSpan={2}>
                Total Weight: {(breakdown.totalWeight * 100).toFixed(2)}%
              </td>
              <td className="whitespace-nowrap px-3 py-3 text-right text-sm text-gray-900">
                {breakdown.finalApportionmentPercentage.toFixed(4)}%
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      {/* Chart Type Toggle */}
      <div className="flex justify-center space-x-4">
        <button
          onClick={() => {/* Toggle to bar chart */}}
          className={`px-4 py-2 rounded ${
            chartType === 'bar'
              ? 'bg-blue-600 text-white'
              : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
          }`}
        >
          Bar Chart
        </button>
        <button
          onClick={() => {/* Toggle to pie chart */}}
          className={`px-4 py-2 rounded ${
            chartType === 'pie'
              ? 'bg-blue-600 text-white'
              : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
          }`}
        >
          Pie Chart
        </button>
      </div>
    </div>
  );
};

export default ApportionmentChart;
